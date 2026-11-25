# Kiểm thử bản cập nhật thư viện

Ngày 25/11/2026. Bản này tích hợp các tính năng của ZIP
`BTL_OOP_ThuVien_CapNhat.zip` vào bản đã sửa trước đó.

| Phần | Ca đã chạy |
|---|---|
| Build và khởi động | JDK 17, Maven 3.9.16, biên dịch 61 file Java bằng `mvn clean verify`. `mvn exec:java` mở đúng màn đăng nhập. |
| Database | MySQL 8.4.13. Cài mới 00–14, nâng cấp bản đầu và bản đã chạy 14 cũ, lặp lại migration. Kiểm tra giá thuê, tiền phạt, điểm, phiếu miễn phí và trạng thái email đã gửi. |
| Độc giả | CCCD dạng chuỗi giữ số 0 đầu. Từ chối mã trùng và mã sai khi tạo hoặc đổi mã. Mã cũ vẫn tra cứu, chỉnh sửa và trả sách được. Danh sách và biên nhận che tám chữ số đầu của CCCD. |
| Mã phiếu và giao diện | Mã phiếu mới dùng ngày Việt Nam và ID MySQL. Ngày trên chứng từ khớp bản ghi gốc. Mở hai tab Mượn/Trả, lọc theo mã độc giả và in lại chứng từ qua bản xem trước. |
| BXH và demo 92 | Bốn đầu sách mẫu có cặp lượt/số bản 5/6, 4/5, 2/2, 1/1. Mượn hai bản trên một phiếu tăng một lượt. Nhiều dòng cùng đầu sách trên một phiếu vẫn chỉ tính một lượt. Chạy lại demo sau khi trả sách giữ nguyên phiếu, tồn và điểm. |
| Hồi quy Checkout | Mượn/trả, tranh bản cuối, trả sớm/đúng hạn/muộn, hỏng/mất, chặn trả lặp, ép lỗi SQL để kiểm tra rollback, voucher chỉ dùng một lần và giữ sách đúng lượt. |
| Hồi quy tài khoản | Đăng nhập theo vai trò, khóa/mở khóa, khóa đồng thời và bảo vệ admin cuối cùng. |
| Hồi quy email | SMTP giả lập kiểm tra phạm vi TEST, trạng thái sau khi SMTP chấp nhận, SMTP từ chối hoặc mất kết nối, chống gửi trùng, preview và mẫu dự phòng khi thiếu khóa AI. |

Maven biên dịch thành công. Các kiểm thử nghiệp vụ chạy riêng trên
database và màn hình ảo. Gmail, Gemini và máy in vật lý chưa được chạy
trong đợt này. Dữ liệu thử không phải hồ sơ của thư viện thật.

Xem thứ tự cài đặt và nâng cấp ở `README.md`.
