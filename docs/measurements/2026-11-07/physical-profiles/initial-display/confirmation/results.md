# Xác nhận Baseline Profile và Startup Profile

Cold start khi chưa đăng nhập; thiết bị Redmi/air_in/air:13/TP1A.220624.014/V14.0.8.0.TGQINXM:user/release-keys; thứ tự F1-E1-E2-F2.
Mỗi khối 5 lần, gộp 10 lần/cấu hình; không build giữa các khối; giữ đủ mọi quan sát.

| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| E | 909.420 | 1064.026 |
| F | 839.737 | 946.451 |

E/F: cùng mã app, lazy, R8 và đầu vào Baseline. Cài mới, cài Baseline rồi biên dịch speed-profile; F bật thêm Startup Profile. Macrobenchmark compilation.enabled=false giữ trạng thái đã chuẩn bị; xem external-compilation.log và dexopt-before.txt từng khối.

| So sánh | Ý nghĩa | TTID thay đổi | TTFD thay đổi |
|---|---|---:|---:|
| E/F | Bổ sung Startup Profile | -7.66% | -11.05% |

Dấu âm = thời gian giảm; dấu dương = thời gian tăng. So sánh trung vị, không phải mọi lần đều nhanh/chậm hơn.

| APK | Byte | SHA-256 |
|---|---:|---|
| E | 2746797 | B2664DB3008B69EF71042E20340A12F1B67F72A8B992D10708BADCBD21359CCF |
| F | 2845159 | B92C7C1768E02A2485E429869BE89FA649B09034F183D0E332D4C76CA9763A67 |

| Cấu hình | Chỉ số | n | Trung bình (ms) | Nhỏ nhất (ms) | Lớn nhất (ms) |
|---|---|---:|---:|---:|---:|
| E | TTID | 10 | 1009.276 | 845.003 | 1393.451 |
| E | TTFD | 10 | 1154.204 | 985.093 | 1557.123 |
| F | TTID | 10 | 890.497 | 695.903 | 1468.806 |
| F | TTFD | 10 | 1025.260 | 824.283 | 1653.944 |

Đo trên một thiết bị thật; chưa khái quát sang các điện thoại khác. Chưa thiết lập ý nghĩa thống kê.
Việc Startup Profile được áp dụng được kiểm tra độc lập bằng metadata R8 và SHA-256 DEX, xem startup-verification.json.
