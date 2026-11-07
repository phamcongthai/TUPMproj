# Bộ chuẩn bị báo cáo và demo

Đọc trước **`../android-startup-guide/README.html`** hoặc [hướng dẫn Markdown](../android-startup-guide/README.md). Đây là tài liệu từ đầu đến mã và bằng chứng, dành cho người chưa biết Android.

Các phần đã có: mã tối ưu, module Macrobenchmark, profile sinh từ thực thi, dữ liệu A–G và xác nhận A/E, APK E đã kiểm tra, bảng và biểu đồ tự tính lại. Đây là nghiên cứu triển khai/đánh giá kỹ thuật có sẵn, không phải đề xuất một thuật toán mới.

- [Đề cương 27 trang](report-outline.md): khung nội dung để nhóm tự viết.
- [Kịch bản demo và câu hỏi bảo vệ](demo-and-defense.md): trình tự mở app, đọc mã và kiểm tra dữ liệu.
- [Kết quả tự tính lại](results/results.md), [CSV từng lần đo](results/raw-runs.csv).
- [Kết luận thực nghiệm và giới hạn](../measurements/2026-11-07/confirmed-results.md).

Kết quả chính: TTID trung vị giảm 14,00%, TTFD giảm 25,92%, APK giảm 82,94% ở cặp A/E trên máy ảo. A/E thay đổi nhiều yếu tố cùng lúc nên không dùng làm mức tăng tốc riêng của Baseline Profile. Startup Profile đã triển khai thử ở F, hiện chưa bật mặc định.

Nhóm cần tự hoàn thiện thông tin học phần/thành viên, phân công, phần diễn giải và nhận xét. Nếu chưa đo trên máy thật, ghi đúng là giới hạn; không thêm số liệu dự kiến vào bảng kết quả.
