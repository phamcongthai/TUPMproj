# Xác nhận Baseline Profile và Startup Profile

Cold start khi chưa đăng nhập; thiết bị Redmi/air_in/air:13/TP1A.220624.014/V14.0.8.0.TGQINXM:user/release-keys; thứ tự D1-E1-F1-F2-D2-E2-E3-F3-D3.
Mỗi khối 3 lần, gộp 9 lần/cấu hình; không build giữa các khối; giữ đủ mọi quan sát.

| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| D | 817.918 | 1021.801 |
| E | 730.195 | 961.245 |
| F | 898.611 | 1071.414 |

D: lazy + R8, cùng APK E; cài mới và chuẩn bị verify (không AOT).
E: cài mới, cài Baseline Profile và biên dịch speed-profile. F: thêm Startup Profile, cùng cách chuẩn bị như E. Macrobenchmark compilation.enabled=false giữ nguyên trạng thái đã chuẩn bị; xem external-compilation.log và dexopt-before.txt từng khối.

| So sánh | Ý nghĩa | TTID thay đổi | TTFD thay đổi |
|---|---|---:|---:|
| D/E | Biên dịch theo Baseline Profile | -10.73% | -5.93% |
| E/F | Bổ sung Startup Profile | +23.06% | +11.46% |

Dấu âm = thời gian giảm; dấu dương = thời gian tăng. So sánh trung vị, không phải mọi lần đều nhanh/chậm hơn.

| APK | Byte | SHA-256 |
|---|---:|---|
| E | 2746797 | 05CAC92B78CBD732B4930F60D7F927EDE68356EF3C30FB126F71A1FFFAF12093 |
| F | 2845159 | 0135F36386312E406D134E29F96AEAA02494E8339B34C41C65D595A11D09FC3D |

Đo trên một thiết bị thật; chưa khái quát sang các điện thoại khác. Chưa thiết lập ý nghĩa thống kê.
D/E kiểm soát cùng APK nhưng compilation có thể sử dụng cả profile thư viện; không quy hết cho riêng quy tắc app.
A/F là tác động tổng hợp; không phải mức cải thiện riêng do hai loại profile.
Việc Startup Profile được áp dụng được kiểm tra độc lập bằng metadata R8 và SHA-256 DEX, xem startup-verification.json.
