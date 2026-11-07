-- Inclusive durations. Rows can overlap and must not be added together.
-- Limit to this app's main thread; retain timestamps for audit of startup scope.
WITH phases AS (
 SELECT s.id, s.name, s.ts, s.dur, t.utid,
 CASE WHEN s.name GLOB 'OpenDexFilesFromOat*' THEN 'open_dex'
      WHEN s.name = 'bindApplication' THEN 'bind_application'
      WHEN s.name = 'activityStart' THEN 'activity_start'
      WHEN s.name = 'activityResume' THEN 'activity_resume'
      WHEN s.name = 'postAndWait' THEN 'render_wait'
      WHEN s.name = 'reportFullyDrawn() for ComponentActivity' THEN 'fully_drawn'
 END AS phase
 FROM slice s JOIN thread_track tt ON s.track_id = tt.id
 JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
 WHERE p.name = 'com.example.jetpackcompose.benchmark'
 AND t.is_main_thread = 1 AND s.dur > 0
)
SELECT phase, name, ts, dur / 1e6 AS wall_ms,
 COALESCE((SELECT SUM(MIN(sc.ts+sc.dur, phases.ts+phases.dur)-MAX(sc.ts,phases.ts))
 FROM sched sc WHERE sc.utid = phases.utid AND sc.dur > 0
 AND sc.ts < phases.ts+phases.dur AND sc.ts+sc.dur > phases.ts),0)/1e6 AS scheduled_cpu_ms
FROM phases WHERE phase IS NOT NULL
AND ts <= (SELECT MIN(ts) FROM phases WHERE phase = 'fully_drawn') ORDER BY ts;
