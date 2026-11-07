# Kịch bản demo khoảng 7 phút và câu hỏi bảo vệ

## Chuẩn bị trước buổi trình bày

Mở sẵn HTML hướng dẫn, hai hình TTID/TTFD, CSV và JSON gốc. Mở repo trong Android Studio đúng thư mục `food-app`; chuẩn bị emulator đã boot hoặc điện thoại mở khóa. Dùng APK E đã lưu để tránh phải chờ R8 build tại buổi bảo vệ. Không đăng nhập tài khoản thật hoặc gửi giao dịch chỉ để trình diễn startup.

Kiểm tra trước luồng Login → Register → Login. Chuẩn bị APK A/E và đúng test APK nếu muốn chạy lại benchmark; toàn bộ A–G có thể mất nhiều thời gian, không nên hứa chạy hết trong vài phút. Khi trình bày dữ liệu đã thu, nói rõ đó là kết quả thực nghiệm ngày 07/11/2026, không nói là số đang đo trực tiếp.

## Trình tự

| Thời gian dự kiến | Thao tác | Điểm cần giải thích |
|---|---|---|
| 0:00–0:45 | Mở sơ đồ 2 và app Login | Phạm vi là chưa có token mở đến Login, không phải toàn bộ đặt món |
| 0:45–2:15 | Mở sơ đồ trước/sau; chỉ AuthRepository và AuthPreference | Lazy dời chi phí chưa cần; có token vẫn kiểm tra tài khoản |
| 2:15–3:15 | Mở Gradle, generator và sơ đồ 6–7 | R8 khác ART; Baseline khác Startup; profile sinh từ chạy thật |
| 3:15–4:00 | Bấm Đăng ký rồi quay Login | Kiểm tra điều hướng sau sửa; chưa kiểm thử đăng nhập/thanh toán thành công |
| 4:00–5:30 | Mở JSON A1, E1 và hai biểu đồ | 10 cold starts mỗi cấu hình gộp ABBA, đủ mọi lần, trung vị/min–max |
| 5:30–6:15 | Chạy Export-StartupResults.ps1 | Tính lại từ dữ liệu gốc, không phát sinh số đo mới |
| 6:15–7:00 | Mở bảng A–G và giới hạn | 14%/25,92% là gói E; F chưa thấy lợi ích thêm; cần xác nhận máy thật |

Lệnh export và cài APK có trong guide phần 13. Mã vẽ sơ đồ là `build_diagrams.py`; không cần chạy Python lúc bảo vệ vì đã có PNG/SVG/HTML offline.

## Câu hỏi có thể gặp

**1. Đề tài có gì ngoài làm một app đặt đồ ăn?**

App có sẵn là đối tượng nghiên cứu. Phần triển khai thêm là phân tích đường khởi động, lazy initialization, tối ưu build, sinh/đóng profile, module đo và thiết kế thực nghiệm đối chứng.

**2. Baseline Profile có phải thuật toán do nhóm đề xuất?**

Không. Đây là kỹ thuật Android cung cấp. Nhóm nghiên cứu cơ chế, tích hợp và đánh giá trong một ứng dụng cụ thể.

**3. Vì sao không dùng thời gian Install successfully?**

Đó là cài APK. Người dùng mở app là sự kiện khác; nghiên cứu dùng StartupTimingMetric để đo TTID/TTFD.

**4. TTID và TTFD khác nhau thế nào?**

TTID là đến khung hình đầu. TTFD dùng mốc app báo sẵn sàng; ở đây ReportDrawn tại Login. Không dùng nó để nói đã tải xong thực đơn.

**5. Lazy có loại bỏ hoàn toàn chi phí không?**

Không, chi phí chuyển đến lần đầu cần đối tượng. Cần nghiên cứu thêm độ trễ thao tác đầu tiên để đánh giá toàn hành trình.

**6. Vì sao không chỉ đo một lần?**

Môi trường có nhiễu. Nhóm giữ nhiều lần đo và công bố phân bố; trung vị giảm ảnh hưởng của các lần cực chậm nhưng không xóa nhiễu hay thay thế kiểm định thống kê.

**7. 25,92% có phải mức tăng tốc riêng của Baseline Profile?**

Không. A/E thay đổi lazy, R8 và compilation/profile. Để tách riêng phải xác nhận các cặp như D/E trong nhiều đợt phù hợp.

**8. Tại sao bảng thăm dò có cấu hình tối ưu lại chậm hơn?**

Có biến động tải host do build giữa lượt, đặc điểm emulator và các đánh đổi kỹ thuật. Nhóm công bố các giá trị, rồi đo xác nhận A/E riêng với Gradle đã dừng. Chưa có bằng chứng để quy mọi chênh lệch thăm dò cho mã.

**9. Startup Profile đã làm mà không bật thì có thất bại không?**

Đã triển khai, kiểm tra đầu vào build và thử F. Lượt thăm dò chưa thấy lợi ích bổ sung, APK tăng. Chọn chưa bật là quyết định theo bằng chứng hiện có; không kết luận Startup Profile vô dụng nói chung.

**10. A có đúng là app gốc không?**

A tái hiện eager và tắt tối ưu bằng cờ trong cùng bộ mã có instrumentation. Nó không phải APK nguyên bản hoàn toàn; cả hai cùng có mốc ReportDrawn và hạ tầng đo.

**11. Đo trên máy ảo có kết luận cho điện thoại được không?**

Chưa. Nó chứng minh quy trình hoạt động và cho kết quả trong môi trường đó. Cần lặp lại trên thiết bị thật, nhiều đợt, ghi điều kiện rõ ràng.

**12. Đã kiểm tra chức năng nào sau minify?**

Đã mở Login và điều hướng Register rồi quay lại. Chưa có bằng chứng end-to-end cho tài khoản, Google, menu, đặt hàng hoặc thanh toán; keep rule Gson vẫn cần kiểm thử các luồng dữ liệu đó khi mở rộng.

**13. Đã giảm RAM/pin chưa?**

Chưa đo. APK nhỏ hơn không tự chứng minh RAM/pin thấp hơn.

**14. Profile dùng khi nào?**

Trong benchmark, Partial/Require chuẩn bị profile/compilation trước lượt đo. Trong cài đặt thực tế còn phụ thuộc cách phân phối/cài và ART; không coi sideload bình thường luôn có ngay trạng thái AOT tương đương.

**15. Bằng chứng nào giúp người khác kiểm tra kết luận?**

JSON chứa toàn bộ runs và context, instrumentation log, cấu hình A–G, APK/hash, script tái lập và script tính lại. Hash nhận diện tệp; chất lượng thiết kế thí nghiệm vẫn cần đánh giá riêng.
