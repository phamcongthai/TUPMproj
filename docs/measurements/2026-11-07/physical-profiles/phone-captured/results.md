# Xác nhận Baseline Profile và Startup Profile

**Phiên bị nhiễu: người dùng xác nhận có sử dụng điện thoại trong lúc đo. Giữ dữ liệu để tham khảo, không dùng để kết luận hiệu quả. Audit PASS không kiểm tra điều kiện để yên máy.**

Cold start khi chưa đăng nhập; thiết bị Redmi/air_in/air:13/TP1A.220624.014/V14.0.8.0.TGQINXM:user/release-keys; thứ tự D1-E1-F1-F2-D2-E2-E3-F3-D3.
Mỗi khối 3 lần, gộp 9 lần/cấu hình; không build giữa các khối; giữ đủ mọi quan sát.

| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| D | 848.827 | 1086.622 |
| E | 747.401 | 869.677 |
| F | 784.759 | 926.933 |

D: lazy + R8, cùng APK E; cài mới và chuẩn bị verify (không AOT).
E: cài mới, cài Baseline Profile và biên dịch speed-profile. F: thêm Startup Profile, cùng cách chuẩn bị như E. Macrobenchmark compilation.enabled=false giữ nguyên trạng thái đã chuẩn bị; xem external-compilation.log và dexopt-before.txt từng khối.

| So sánh | Ý nghĩa | TTID thay đổi | TTFD thay đổi |
|---|---|---:|---:|
| D/E | Biên dịch theo Baseline Profile | -11.95% | -19.97% |
| E/F | Bổ sung Startup Profile | +5.00% | +6.58% |

Dấu âm = thời gian giảm; dấu dương = thời gian tăng. So sánh trung vị, không phải mọi lần đều nhanh/chậm hơn.

| APK | Byte | SHA-256 |
|---|---:|---|
| E | 2746797 | 7198ECDD1D48FF00EB291FAFFCC4A8B3F3806A6EE99BD6509D3A3285C4831A49 |
| F | 2845159 | 6E98DD1910B8F4F7278738BD9BF84C771535D38F567933D7802E8CA7AE458103 |

Đo trên một thiết bị thật; chưa khái quát sang các điện thoại khác. Chưa thiết lập ý nghĩa thống kê.
D/E kiểm soát cùng APK nhưng compilation có thể sử dụng cả profile thư viện; không quy hết cho riêng quy tắc app.
Việc Startup Profile được áp dụng được kiểm tra độc lập bằng metadata R8 và SHA-256 DEX, xem startup-verification.json.
