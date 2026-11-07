# Kiểm chứng riêng Baseline Profiles và Startup Profiles

## Thiết kế lượt thử tiếp theo (ghi trước khi đo)

- Giữ nguyên mã ứng dụng, lazy initialization, R8 và resource shrinking ở mọi cấu hình.
- D: APK không có Startup Profile, `CompilationMode.None()`.
- E: chính APK của D, `CompilationMode.Partial(Require, warmupIterations=0)`.
- F: cùng mã nguồn và Baseline Profile với E, bật thêm Startup Profile, cùng chế độ Partial/Require.
- D/E kiểm tra lợi ích biên dịch theo Baseline Profile (bao gồm quy tắc thư viện được đóng gói).
- E/F kiểm tra tác động bổ sung Startup Profile. Không dùng A/F để quy công cho hai profile.
- Thứ tự: D1, E1, F1, F2, D2, E2, E3, F3, D3; mỗi khối 3 lần cold start, tổng 9 lần/cấu hình.
- Không build giữa các khối; lưu APK, SHA-256, JSON từng lượt, trace và log. Đây là thử nghiệm thăm dò trên máy ảo, cần xác nhận bằng máy thật.

## Điều chỉnh có căn cứ

Trước đây Baseline và Startup dùng cùng hành trình đến khi thấy Login. Lần này tách:

1. `initialDisplayStartup`: `includeInStartupProfile=true`, mở app và chờ hiển thị ban đầu bằng `startActivityAndWait()`.
2. `loggedOutStartup`: `includeInStartupProfile=false`, mở app và chờ nội dung Login; dùng cho Baseline.
3. Baseline là hợp các quy tắc quan sát được trong hai hành trình, bảo toàn các cờ H/S/P. Startup chỉ nhận quy tắc từ hành trình đầu tiên. Không thêm phương thức giả, không tự gán cờ hot.

Giới hạn thu thập tăng từ 5 lên 15 vòng, giữ yêu cầu ổn định 3 vòng. Giới hạn vòng không tự chứng minh profile đã hội tụ; cần đọc log thu thập.

Đây là giả thuyết cần kiểm tra: thu hẹp phạm vi Startup có thể cải thiện bố trí DEX. Không khẳng định thu hẹp luôn nhanh hơn hoặc profile cũ không hợp lệ.

## Đọc trace cũ

Script `scripts/analyze_profile_traces.py` cùng `scripts/profile-trace-phases.sql` phân tích 20 trace E/F cũ. Kết quả giới hạn đến lần `reportFullyDrawn()` đầu tiên nằm ở `build/startup-study/profile-diagnosis-startup-only-20261107`.

Một phân tích ban đầu trên toàn trace thấy đoạn `postAndWait` khoảng 1,15 giây. Đối chiếu timestamp cho thấy nó nằm **sau** mốc fully drawn; không dùng đoạn đó để giải thích TTID/TTFD. Thư mục `profile-diagnosis-20261107` là bản toàn trace đã được thay thế cho mục đích phân tích khởi động.

Trung vị `OpenDexFilesFromOat`: E 185,554 ms, F 198,004 ms; thời gian main thread được lập lịch trên CPU trong đoạn này tương ứng 70,405 và 74,175 ms. Các slice lồng nhau nên không cộng thành tổng. Chênh lệch wall time/CPU time không đủ xác định nguyên nhân chờ. Trace processor báo `unknown_extension_fields` trên API 37; đây là chẩn đoán bổ sung, không thay thế số đo Macrobenchmark.

## Tài liệu chính thức đã đối chiếu

- [Khác biệt Baseline và Startup Profiles](https://developer.android.com/topic/performance/baselineprofiles/difference-baseline-startup): Startup tập trung đường đi hiển thị ban đầu; Baseline bao phủ rộng hơn.
- [Tạo Startup Profiles](https://developer.android.com/topic/performance/startupprofiles/dex-layout-optimizations): yêu cầu R8; đường mã khởi động nên nằm trong DEX đầu.
- [Xác nhận Startup Profiles](https://developer.android.com/topic/performance/baselineprofiles/confirm-startup-profiles): kiểm tra metadata R8 và checksum DEX.
- [Đo Baseline bằng Macrobenchmark](https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile): so sánh None với Partial/Require; đo hiệu năng trên thiết bị thật.
- [Gỡ lỗi Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/debug-baseline-profiles): phân biệt profile được đóng gói, cài đặt và biên dịch; có thể thu thập profile trên máy ảo.

Kết quả của lượt mới sẽ được lưu riêng sau khi hoàn tất; các kết quả cũ không bị thay thế. README.md và README.html đang được giữ nguyên theo yêu cầu.

## Trạng thái triển khai mới

Thu thập hoàn tất, cả hai test thành công; log xác nhận ổn định cho cả hai hành trình. Startup có 17.416 quy tắc riêng biệt; Baseline hợp nhất có 17.712 quy tắc. Phạm vi gần nhau vì Login cũng đã chạy trong hành trình hiển thị ban đầu. Không xem việc tách hành trình là bằng chứng tăng tốc.

Cặp APK đã build thành công ở `build/startup-study/profile-refined-pair-20261107`. E không có DEX đánh dấu startup; F có DEX đầu đánh dấu startup và checksum khớp APK. DEX đầu của F có 1.596.508 byte, 1.863 lớp; profile nhị phân 10.756 byte. SHA-256 APK F: `0135f36386312e406d134e29f96aeaa02494e8339b34c41c65d595a11d09fc3d`.

Bằng chứng nhỏ được sao chép vào [thư mục đo](measurements/2026-11-07/profile-refinement/). APK thử nghiệm mới: `build/artifacts/food-app-refined-startup-profile.apk`. Đây là xác nhận triển khai, chưa phải kết luận hiệu năng.

Ở mốc chuẩn bị ban đầu, điện thoại model 23124RN87C, Android 13/API 33, ARM64 bị hệ thống trả `INSTALL_FAILED_USER_RESTRICTED`; thao tác input bị chặn quyền `INJECT_EVENTS`. Sau khi chủ máy hoàn tất tùy chọn USB và xác nhận cài app, các phép đo máy thật bên dưới đã chạy được.

## Tiếp tục sau khi tạm dừng

Phiên sáng lưu được 21 lượt hoàn chỉnh ở `build/startup-study/physical-profiles-20261107-data`; F3 chưa có báo cáo hoàn chỉnh và D3 chưa chạy. Không dùng phần thiếu này như một thí nghiệm 27 lượt hoàn chỉnh.

Sau khi kết nối lại cùng điện thoại, chạy lại trọn thứ tự đã định bằng chính hai APK cũ, lưu riêng ở `build/startup-study/physical-profiles-resumed-20261107`. Không gộp dữ liệu hai phiên để tránh ảnh hưởng của khoảng nghỉ, nhiệt độ và trạng thái máy.

Android 13 trên Xiaomi chặn bước cài lại bên trong Macrobenchmark. Vì vậy, mỗi khối được gỡ/cài mới qua ADB; ghi skip file của ProfileInstaller; D biên dịch `verify`; E/F cài profile bằng broadcast (phải trả mã 1) rồi biên dịch `speed-profile`. Macrobenchmark chạy với `androidx.benchmark.compilation.enabled=false`. Log ART trước mỗi khối xác nhận trạng thái thực tế; đây là chuẩn bị ngoài phép đo theo [tham số chính thức](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args#androidxbenchmarkcompilationenabled).

Thử nghiệm tiếp theo đã chuẩn bị, nếu Startup Profile mặc định chưa cho cải thiện rõ: giữ nguyên Baseline và mã app, chỉ loại quy tắc phương thức không có cờ ART `S` khỏi Startup Profile; vẫn giữ toàn bộ quy tắc lớp đã quan sát. Script `scripts/filter_startup_phase.py` loại 831/17.416 quy tắc riêng biệt, còn 16.585. Đây là giả thuyết về tập trung bố trí DEX vào pha startup, dựa trên [ngữ nghĩa H/S/P](https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure#rule-syntax), không phải khuyến nghị Android rằng phép lọc này luôn tốt hơn.

Kế hoạch xác nhận biến thể: E1-F1-F2-E2, 5 lượt/khối, tổng 20 cold start; E giữ APK Baseline cũ, F là APK chỉ thay Startup Profile. TTID là chỉ tiêu chính cho bố trí mã khởi động; báo cả TTFD, mọi quan sát và mọi thử nghiệm, kể cả trường hợp không cải thiện. Không sửa thời gian chờ, hành trình Login hoặc cấu hình R8/lazy để tạo lợi thế cho F.

## Kết quả đã hoàn tất

- Phiên đủ D/E/F: 27 lượt, kiểm tra độc lập PASS; D→E TTID -10,73%, TTFD -5,93%; E→F TTID +23,06%, TTFD +11,46%.
- Biến thể S-phase: lần cài đầu bị khóa màn hình, chưa có quan sát. Sau khi chủ máy mở khóa, chạy đủ 20 lượt ở `build/startup-study/physical-startup-phase-unlocked-20261107`, kiểm tra độc lập PASS; E→F-S TTID +9,67%, TTFD +10,12%.
- Không gộp hai phiên, không bỏ lượt chậm hoặc lượt đầu. 21 lượt trước khi tạm dừng giữ riêng.
- Cấu hình Startup trong nguồn được trả lại đúng file đã thu thập ban đầu (SHA-256 `587b3939f456287499d1fcd7c0e83dba0dfad6dcc416bfbe4471ebbfc72c7bba`). Biến thể S-phase giữ riêng để tái lập, không được quảng bá là nhanh hơn.
- Xem [báo cáo ngắn và dữ liệu gốc](physical-profile-results.md). Chưa chứng minh lợi ích bổ sung của Startup Profile trong điều kiện đã đo; không khẳng định nguyên nhân suy giảm khi chưa kiểm chứng riêng.

## Kiểm tra tiếp: profile thu trực tiếp trên Android 13

Profile hai phiên trên được thu trên emulator API 37. Đây là cấu hình hợp lệ, nhưng bộ quy tắc có thể chưa bao phủ các nhánh tương thích chạy trên điện thoại API 33. Đây là giả thuyết về độ bao phủ, chưa phải nguyên nhân suy giảm đã được chứng minh.

`scripts/Capture-PhoneStartupProfile.ps1` thu lại trên đúng điện thoại bằng APK không minify, không debug, cùng cờ lazy. Thay bước reset không root bằng gỡ/cài mới riêng package benchmark; ghi skip file; bắt đầu ở `verify`; mở app đến Login; gửi SAVE_PROFILE (mã 12); gộp profile bằng `speed-profile`; xuất HRF bằng `pm dump-profiles --dump-classes-and-methods`. Thiết bị này cho phép lệnh dump; script dừng nếu thiết bị khác không hỗ trợ, không cố root máy.

Sau 7 vòng, ba snapshot cuối giống nhau theo so sánh tập quy tắc phân biệt hoa/thường. Giữ snapshot nguyên bản; loại quy tắc array/inline-cache theo cách `filterProfileRulesToTargetP` của AndroidX trước khi đưa vào build. Baseline hợp nhất có 17.977 quy tắc riêng biệt, trong đó 433 quy tắc chưa có ở bộ emulator; ví dụ phương thức `EdgeToEdgeApi29.setUp` thực sự được ghi nhận với cờ `SP` trên điện thoại.

Dữ liệu thu ở `build/startup-study/physical-api33-capture-20261107/capture`; cặp APK ở `build/startup-study/physical-api33-pair-normalized-20261107`. Hai profile dùng cùng hành trình khởi động đến Login; Baseline giữ toàn bộ quy tắc quan sát, Startup nhận bộ startup đã thu, không thêm phương thức hoặc cờ hot giả.

Kế hoạch đo trước khi chạy: giữ nguyên source app/R8/lazy, dùng profile thu trên API 33 cho cả E/F; D/E cùng APK, D `verify`, E/F cài Baseline rồi `speed-profile`; thứ tự D1-E1-F1-F2-D2-E2-E3-F3-D3, ba lượt/khối, 27 lượt. Giữ riêng phiên này, không gộp hoặc so sánh chéo trung vị với hai phiên trước. Việc thu lại cũng thay đổi các cờ ART và tập quy tắc; không quy toàn bộ chênh lệch giữa các phiên cho riêng phiên bản Android.

## Điều kiện phiên 27 lượt dùng profile điện thoại

Sau khi hoàn tất, người dùng xác nhận đã sử dụng điện thoại trong khoảng 15:34–15:48. Giữ nguyên tất cả số liệu nhưng đánh dấu phiên này bị nhiễu; không dùng mức thay đổi 11,95%/19,97% hoặc 5,00%/6,58% để kết luận hiệu quả nhân quả. Audit PASS xác nhận dữ liệu và trạng thái biên dịch, không xác nhận điện thoại được để yên. Cần chạy lại đủ thứ tự đã định trong phiên riêng, không chọn hoặc bỏ riêng những lượt nghi bị can thiệp.

## Phiên đo lại khi người dùng để máy tự chạy

Người dùng xác nhận sẵn sàng để yên máy trước khi chạy lại. Giữ đúng cặp APK API 33: E `7198ECDD1D48FF00EB291FAFFCC4A8B3F3806A6EE99BD6509D3A3285C4831A49`, F `6E98DD1910B8F4F7278738BD9BF84C771535D38F567933D7802E8CA7AE458103`. Thứ tự và số lượt giữ như kế hoạch 27 lượt trước; không chọn lượt và không trộn phiên.

- D: TTID 1068.374692 ms; TTFD 1380.999077 ms.
- E: TTID 930.031923 ms; TTFD 1065.297461 ms.
- F: TTID 1040.692846 ms; TTFD 1179.933154 ms.
- D→E giảm trung vị 12,95% / 22,86%; E→F tăng 11,90% / 10,76%. Chưa chứng minh lợi ích riêng của Startup. Không giải thích thay đổi giữa phiên bị nhiễu và phiên để yên chỉ bằng một nguyên nhân.
- Audit độc lập PASS cho đủ 27 lượt/9 khối, JSON/CSV, APK, ART trước đo và trace. Kết quả ở `build/startup-study/physical-api33-idle-20261107`, bản xuất ở `docs/measurements/2026-11-07/physical-profiles/phone-captured-idle`.
- Bộ profile đang dùng trong source là bản thu trên điện thoại API 33: Baseline SHA `bcef6dddc92db3ff75549c698850ddb8461266a147457602ec76eebbe7f96e83`; Startup SHA `3115fd982a1cf73ed3b6e1e56375fbdb4282533259274d44f036c1bf04a7a2f5`. Không dùng biến thể lọc S làm mặc định.
- Runner dùng trong phiên mới đã sửa riêng `LoginSmokeTest` để mở app qua shell, tránh giới hạn mở Activity từ tiến trình nền trên OEM. `StartupBenchmark` và APK app không đổi; hash runner/source được lưu riêng cho từng phiên. Lỗi smoke trước đó được giữ nguyên trong phiên bị nhiễu, không tính thành lượt đo hiệu năng.

Tái lập phép đo với APK đã lưu, sau khi mở khóa và để máy tự chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Confirm-ProfileEffects.ps1 -StudyDirectory build/startup-study/physical-api33-pair-normalized-20261107 -StartupApk build/startup-study/physical-api33-pair-normalized-20261107/F-baseline-startup-aot/app-benchmark.apk -OutputDirectory build/startup-study/physical-repeat-new -Iterations 3 -Serial 5DH6X4WOJJGQAIJB -ProfilesOnly -ExternalCompilation -ConfirmBenchmarkInstallDialogs
build/guide-tools/python/python.exe scripts/audit_profile_study.py build/startup-study/physical-repeat-new
```

Dùng thư mục output mới, không ghi đè phiên cũ. Serial trong lệnh là điện thoại của phiên này; đổi khi dùng thiết bị khác. Cài đặt giữ sáng qua USB được quản lý tạm trong phiên và phải trả lại giá trị ban đầu sau khi xong.

## Thử nghiệm tiếp: thu Startup ngay sau hiển thị ban đầu

Giả thuyết và chỉ số chính được ghi trong `build/startup-study/physical-initial-display-20261107/plan.txt` trước đo. Profile Startup trước đây chờ 5 giây rồi dump UI trước khi flush; profile mới flush ngay khi `am start -W` trả về, không chờ/dump UI trước flush. Vẫn cần 1 giây cho ART lưu bất đồng bộ; không coi đây là ranh giới thời gian tuyệt đối chính xác đến từng mili giây.

Thu 10 vòng đầu đạt ổn định 2; tiếp tục cùng APK/thiết bị/trạng thái tại vòng 11 đạt ba tập quy tắc cuối giống hệt nhau, kiểm tra phân biệt hoa/thường. Không chọn một vòng thuận lợi theo số thời gian. Startup mới có 17.249 dòng so với 18.037 trước; giữ toàn bộ dòng quan sát hợp lệ, không thêm wildcard/phương thức/cờ giả. Baseline gộp bộ rộng cũ và quy tắc mới thành 18.047 quy tắc riêng biệt, dùng giống nhau cho E/F. Vì vậy không so phần trăm bằng E của phiên cũ.

Cặp APK đã build và kiểm tra DEX: F SHA `b92c7c1768e02a2485e429869be89fa649b09034f183d0e332d4c76ca9763a67`; DEX đầu startup 1.584.720 byte, MainActivity có mặt, SHA khớp metadata. Mã app/lazy/R8 không đổi. Dự kiến E1-F1-F2-E2, 5 cold start/khối, 20 lượt. TTID trung vị là chỉ số chính vì thử nghiệm nhắm hiển thị ban đầu; TTFD và mọi số gốc vẫn báo đầy đủ, không che đánh đổi.

Phân tích trace của PHIÊN API33 ĐỂ YÊN TRƯỚC cho openDex E/F 65,746/56,939 ms, CPU main từ bindApplication tới reportFullyDrawn 175,936/177,057 ms. Chưa thể khẳng định mở thêm DEX là nguyên nhân suy giảm. Thread ngủ/chờ đáng kể nhưng chưa cô lập được nguyên nhân; không cộng các pha lồng nhau hoặc cộng trung vị của những phân bố khác nhau. Chẩn đoán mới nằm trong thư mục `initial-display/diagnosis`; đây là đọc trace cũ trên processor emulator, không phải đo tốc độ trên emulator.

Phiên thăm dò hiển thị ban đầu đã đủ 20 lượt và audit PASS: E TTID 895,963 / TTFD 1014,992 ms; F TTID 712,409 / TTFD 827,044 ms. Thay đổi trung vị −20,49% / −18,52%. Đây là tín hiệu cải thiện, chưa kết luận từ một phiên. Quyết định chạy xác nhận độc lập trước khi xem số mới: cùng APK, đảo thứ tự F1-E1-E2-F2, 5 lượt/khối, 20 lượt. Giữ hai phiên riêng, không gộp trung vị; TTID vẫn là chỉ số chính và luôn báo TTFD. Phương thức benchmark và APK không đổi; driver chỉ thêm tham số PairOrder để đảo thứ tự, hash driver từng phiên được lưu.

## Xác nhận đảo thứ tự đã hoàn tất

Cùng cặp APK và benchmark, F1-E1-E2-F2, 5 lượt/khối, 20 cold start mới. E: TTID trung vị 909.420461 ms, TTFD 1064.0259615 ms; F: TTID 839.736577 ms, TTFD 946.4511925 ms. Giảm 7,66245% / 11,04999%. Cả hai phiên mới cho trung vị TTID và TTFD thấp hơn khi thêm Startup; mức giảm khác nhau, không quảng bá mức thăm dò 20,49% thành mức ổn định cho mọi lần mở app.

Hai audit đều PASS đủ 20 lượt. `cross-study-check.json` PASS: cùng APK E/F, runner APK, mã StartupBenchmark, đầu vào profile và fingerprint; thứ tự khác đúng kế hoạch. Không gộp 40 lượt để tính một mức tăng tốc, không bỏ lượt chậm và giữ các phiên Startup chưa có cải thiện trước đó. Đây là lặp lại mô tả trên một thiết bị; chưa thiết lập ý nghĩa thống kê. Chưa cô lập riêng nguyên nhân khiến bộ cũ không cải thiện, vì giữa các cặp build còn khác Baseline hợp nhất và các quy tắc đã thu.

APK mặc định nguồn hiện khớp bộ hiển thị ban đầu: Baseline SHA `3e84d0048de7dfbf464d0853375f01d8af6ee2d1d2c1449ba44a89dd3e705739`; Startup SHA `05483c3fcafc5f6499fed8bfc0d15e3e2e5c205294f82a37bf196983036ef849`. APK F hiện tại ở `build/artifacts/food-app-initial-display-startup-profile.apk`, SHA `B92C7C1768E02A2485E429869BE89FA649B09034F183D0E332D4C76CA9763A67`. Đã kiểm tra đúng APK đang cài, navigation smoke PASS, ProfileInstaller result 1 và compile speed-profile thành công; cài đặt giữ sáng USB trả lại 0. Final launch chỉ là sanity check, không cộng vào đo hiệu năng.

Tái lập xác nhận của bộ mới, dùng thư mục output trống và để máy tự chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Confirm-ProfileEffects.ps1 -StudyDirectory build/startup-study/physical-initial-display-20261107/pair -StartupApk build/startup-study/physical-initial-display-20261107/pair/F-baseline-startup-aot/app-benchmark.apk -OutputDirectory build/startup-study/initial-display-repeat-new -Iterations 5 -Serial 5DH6X4WOJJGQAIJB -ProfilePairOnly -PairOrder FEEF -ExternalCompilation -ConfirmBenchmarkInstallDialogs
build/guide-tools/python/python.exe scripts/audit_profile_study.py build/startup-study/initial-display-repeat-new
```

`PairOrder EFFE` tái lập thứ tự thăm dò. Dùng đúng serial thiết bị và giữ máy mở khóa; không chạy các phiên song song trên cùng điện thoại. Source/profile hiện tại không đồng nghĩa các APK cũ tự đổi theo: đọc SHA và metadata của từng phiên.
