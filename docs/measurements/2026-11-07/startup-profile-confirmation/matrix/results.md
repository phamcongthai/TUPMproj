# Xác nhận Baseline Profile và Startup Profile

Cold start khi chưa đăng nhập, API 37 SwiftShader; thứ tự A1-D1-E1-F1-F2-E2-D2-A2.
Mỗi khối 5 lần, gộp 10 lần/cấu hình; không build giữa các khối; giữ đủ mọi quan sát.

| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| A | 2589.106 | 3795.356 |
| D | 1876.786 | 2734.269 |
| E | 1903.714 | 2362.234 |
| F | 1155.765 | 1372.955 |

A: eager, không R8, None. D: lazy + R8 + profile đóng gói nhưng None.
E: cùng APK D, Partial/Require. F: thêm Startup Profile, cùng lazy/R8/Partial như E.

| So sánh | Ý nghĩa | TTID thay đổi | TTFD thay đổi |
|---|---|---:|---:|
| D/E | Biên dịch theo Baseline Profile | +1.43% | -13.61% |
| E/F | Bổ sung Startup Profile | -39.29% | -41.88% |
| A/F | Gói tối ưu đầy đủ so đối chứng | -55.36% | -63.83% |

Dấu âm = thời gian giảm; dấu dương = thời gian tăng. So sánh trung vị, không phải mọi lần đều nhanh/chậm hơn.

| APK | Byte | SHA-256 |
|---|---:|---|
| A | 16096962 | 01D12F8FFC6D429040CEE603F7D66D3161C786CADD44389B5D8EBB706B840C09 |
| E | 2746797 | BE4C255E2EAEF8D6D46C6E6BBC21BD9413D7BF0C7A9D1CDBF64E7CD716CC7A67 |
| F | 2845159 | B94DEDE673758DB0AB1723F1CE751630B8A340491D0154D8BB7ABBC88E25C52D |

Đây là phép đo máy ảo trong một phiên. Chưa thiết lập ý nghĩa thống kê hoặc khả năng khái quát sang máy thật.
D/E kiểm soát cùng APK nhưng compilation có thể sử dụng cả profile thư viện; không quy hết cho riêng quy tắc app.
A/F là tác động tổng hợp; không phải mức cải thiện riêng do hai loại profile.
Việc Startup Profile được áp dụng được kiểm tra độc lập bằng metadata R8 và SHA-256 DEX, xem startup-verification.json.
