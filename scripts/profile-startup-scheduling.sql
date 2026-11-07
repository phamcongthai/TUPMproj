-- App main-thread interval only: bindApplication to first reportFullyDrawn.
-- Thread-state totals are disjoint; named slices are inclusive, not additive.
WITH app AS (
 SELECT t.utid FROM thread t JOIN process p USING(upid)
 WHERE p.name='com.example.jetpackcompose.benchmark' AND t.is_main_thread=1
), bounds AS (
 SELECT t.utid, MIN(CASE WHEN s.name='bindApplication' THEN s.ts END) AS lo,
 MIN(CASE WHEN s.name='reportFullyDrawn() for ComponentActivity' THEN s.ts END) AS hi
 FROM slice s JOIN thread_track tt ON s.track_id=tt.id JOIN app t ON tt.utid=t.utid
 GROUP BY t.utid
), states AS (
 SELECT 'state:'||st.state AS phase, st.state AS name, b.lo AS ts,
 SUM(MIN(st.ts+st.dur,b.hi)-MAX(st.ts,b.lo))/1e6 AS wall_ms,
 0.0 AS scheduled_cpu_ms
 FROM thread_state st JOIN bounds b USING(utid)
 WHERE st.dur>0 AND st.ts<b.hi AND st.ts+st.dur>b.lo
 GROUP BY st.state,b.utid
), totals AS (
 SELECT 'app_main_interval' AS phase, 'bindApplication to fully drawn' AS name, b.lo AS ts,
 (b.hi-b.lo)/1e6 AS wall_ms,
 COALESCE((SELECT SUM(MIN(sc.ts+sc.dur,b.hi)-MAX(sc.ts,b.lo))
 FROM sched sc WHERE sc.utid=b.utid AND sc.dur>0 AND sc.ts<b.hi AND sc.ts+sc.dur>b.lo),0)/1e6 AS scheduled_cpu_ms
 FROM bounds b WHERE b.hi>b.lo
)
SELECT * FROM totals UNION ALL SELECT * FROM states;
