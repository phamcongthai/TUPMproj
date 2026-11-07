# Generated startup profiles

Generated on 2026-11-07 using `BaselineProfileGenerator.loggedOutStartup` on the
Medium Phone API 37 emulator. The app was non-debuggable, unminified, with build
optimization off and lazy initialization on. Rules were captured from execution,
not hand-written wildcards.

The journey is launcher -> splash -> logged-out login screen fully drawn.
There are 17,749 HRF rules, including 502 lines referring to the app package.
Because only the startup journey is collected, the same captured HRF is consumed
as baseline and startup input. Additional non-startup journeys should be merged
into baseline only.

Regenerate after changing startup code:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Run-StartupStudy.ps1 -Mode Generate -Emulator
```

AGP source-set configuration in `app/build.gradle.kts` selects these directories.
The experiment properties `appProfiles` and `startupProfile` can exclude them
without editing or deleting the generated files. R8 rewrites original symbols
when building an optimized APK. Device-specific timing results belong in the
measurement reports, not in the generated rule files.
