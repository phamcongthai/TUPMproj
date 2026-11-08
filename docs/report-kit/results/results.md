# Historical emulator results ? combined optimization package

**Historical data only:** the CSV and PNG files in this folder describe the old combined A/E emulator study. They do not isolate either profile. Use the [current physical-device report](../../physical-profile-results.md) and [final project handoff](../../project-handoff.md) for the report/demo. New Startup confirmation observed lower median TTID/TTFD; these old data remain unchanged for transparency.

ABBA: A1 -> E1 -> E2 -> A2. Logged-out cold start. API 37 SwiftShader emulator (recorded study).
All observations retained; median calculated over both blocks together.

| Metric | A median (ms) | E median (ms) | Reduction | A min-max | E min-max |
|---|---:|---:|---:|---:|---:|
| timeToInitialDisplayMs | 1617.202 | 1390.739 | 14.00% | 1317.940-2795.811 | 957.076-2138.505 |
| timeToFullDisplayMs | 2293.035 | 1698.688 | 25.92% | 1945.176-3687.922 | 1139.687-2510.907 |

APK A: 16096962 bytes; E: 2746797 bytes; reduction: 82.94%.

E combines deferred initialization, R8/resource shrinking and Baseline Profile AOT.
This comparison does not isolate the effect of any single technique.
Ranges overlap. No statistical significance or physical-device speedup is established.
The chart lines mark medians, not confidence intervals. Points are individual starts.
At the time of this historical emulator study, the exploratory Startup case did not show a benefit. This is not the current default or final physical-device conclusion.
