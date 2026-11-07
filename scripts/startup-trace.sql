-- Inclusive slice durations: nested slices overlap, so do not add these rows together.
SELECT s.name, ROUND(s.dur / 1000000.0, 3) AS inclusive_ms
FROM slice AS s
JOIN thread_track AS tt ON s.track_id = tt.id
JOIN thread AS t ON tt.utid = t.utid
JOIN process AS p ON t.upid = p.upid
WHERE p.name = 'com.example.jetpackcompose.benchmark'
  AND t.is_main_thread = 1 AND s.dur > 0
ORDER BY s.dur DESC
LIMIT 25;
