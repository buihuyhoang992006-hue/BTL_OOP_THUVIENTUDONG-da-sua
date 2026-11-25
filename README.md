# Hệ thống Quản lý Thư viện Tự động

Bài tập lớn Lập trình hướng đối tượng bằng Java.

Ứng dụng dành cho một thư viện trường hoặc phòng đọc nhỏ,
có một quầy mượn–trả. Java Swing và MySQL chạy trên cùng máy Windows.

## 1. Phạm vi bản đầu

Hai chức năng trọng tâm:

1. Checkout: mượn–trả sách, lịch sử, tình trạng trả và vi phạm.
2. Nhắc hạn qua email: quét sách đến hạn ngày mai, dùng AI soạn
   lời nhắc và lưu kết quả gửi.

Các chức năng hỗ trợ:

- Đăng nhập và phân quyền quản trị viên/thủ thư.
- Quản lý sách.
- Quản lý độc giả.
- Quản lý tài khoản thủ thư, dành cho quản trị viên.
- Tìm sách theo từ khóa trong màn Quản lý sách.
- Đặt trước theo hàng đợi, giữ bản có sẵn cho người đến lượt và gửi email nhận sách.
- Giá thuê, điểm tích lũy, voucher và báo cáo phí thuê.
- Xem/in lại phiếu mượn, biên bản vi phạm và xuất CSV.
- Bảng xếp hạng sách theo lượt mượn.

Độc giả sử dụng mã thẻ tại quầy và nhận email trên thiết bị cá nhân.
Tài khoản đăng nhập ứng dụng dành cho thủ thư và quản trị viên.

AI được dùng để soạn lời nhắc hạn. Bản này chưa có chatbox độc giả,
gợi ý sách bằng AI hoặc bán hàng.

## 2. Nguồn kế thừa và phần hoàn thiện

Dự án được phát triển từ mẫu NhaSachTienTho_FullProject
và bản thư viện BTL_OOP_THUVIENTUDONG ban đầu.

| Nền tảng | Nội dung đã có |
|---|---|
| Mẫu nhà sách | Person trừu tượng, kế thừa, Swing, đăng nhập, JDBC, CRUD và giao dịch hóa đơn |
| Bản thư viện ban đầu | Schema thư viện, model độc giả/phiếu mượn, mượn–trả cơ bản, các panel lịch sử/vi phạm/độc giả và khung gửi email |
| Phần hoàn thiện hiện tại | Gắn các màn vào MainFrame; hoàn thiện trả hỏng/mất, trạng thái hạn trả, chống xử lý lặp, cập nhật tồn kho/vi phạm trong giao dịch; nhắc hạn nền, lưu kết quả gửi và cá nhân hóa lời nhắc bằng Gemini |

Các phần hoàn thiện tiêu biểu:

- DocGia kế thừa Person.
- ChiTietPhieuMuon xác định trạng thái và số ngày quá hạn.
- PhieuMuonDAO xử lý mượn/trả và cập nhật đồng bộ dữ liệu.
- ViPhamDAO ghi nhận vi phạm trong cùng connection giao dịch.
- SachDAO chặn ghi đè tồn kho khi form đang giữ dữ liệu cũ.
- ThongBaoDAO lưu hàng đợi và trạng thái gửi.
- ThongBaoService quét nền, tạo nội dung và điều phối gửi.
- EmailService gửi bằng SMTP, đọc cấu hình từ biến môi trường.
- AiService gọi Gemini API.
- HanhViTraSachService phân nhóm từ lịch sử trả sách.

Giao dịch và kế thừa đã có trong nền mẫu; phần phát triển mở rộng
và áp dụng chúng cho nghiệp vụ thư viện.

## 3. Công nghệ

- JDK 17.
- Maven.
- Java Swing.
- JDBC và MySQL.
- MySQL Connector/J.
- Jakarta Mail.
- Gson.
- Gemini API.

Maven cũng phải chạy bằng Java 17.

Kiểm tra môi trường:

```powershell
java -version
javac -version
mvn -version
```

## 4. Cài đặt database

### Database mới

Chạy các script theo thứ tự:

1. database/00_khoi_tao_database.sql
2. database/01_danh_muc.sql
3. database/02_sach.sql
4. database/03_nguoi_dung.sql
5. database/04_doc_gia.sql
6. database/05_phieu_muon.sql
7. database/06_chi_tiet_phieu_muon.sql
8. database/07_vi_pham.sql
9. database/08_dat_truoc.sql
10. database/09_thong_bao.sql
11. database/10_lich_su_diem.sql
12. database/11_voucher.sql
13. database/12_checkout.sql
14. database/13_nhac_han.sql
15. database/14_doanh_thu_voucher.sql

Đây là thứ tự cho schema mới, trống. Các file từ `00` đến `11`
khởi tạo dữ liệu mẫu. Dùng MySQL 8.4 hoặc tương thích và chạy
toàn bộ nội dung từng script, kể cả các đoạn `DELIMITER`.

### Database đang có dữ liệu

1. Đóng ứng dụng. Xuất bản sao lưu gồm cả cấu trúc lẫn dữ liệu,
   rồi thử cập nhật trên một bản sao trước.
2. Dùng các file trong ZIP mới này. Chạy lần lượt
   `12_checkout.sql`, `13_nhac_han.sql` và
   `14_doanh_thu_voucher.sql` trong `database/`. Có thể chạy lại
   ba file này nếu cần kiểm tra cấu trúc. Không chạy lại `00`–`11`
   trên schema đang dùng.
3. Mở ứng dụng với `db.properties` trỏ đúng schema. Nếu thiếu
   cấu trúc cần thiết, app báo cần cập nhật. Tài khoản MySQL của ứng dụng
   không cần quyền sửa schema trong lúc chạy.

Bản cập nhật không tạo tiền thuê hay mốc xử lý phạt cho giao dịch
cũ khi thiếu dữ liệu. Giá thuê đang lưu, số tiền phạt đã ghi nhận
và các phiếu hiện có được giữ lại. Sách cũ chưa có thông tin giá
thuê cần được thủ thư cấu hình. Nếu script `14` của bản trước
đã thay số liệu, đối chiếu với bản sao lưu hoặc chứng từ gốc.
Script trong ZIP này không thể tự suy ra số tiền ban đầu.

Các file `90_demo_checkout.sql`, `91_demo_nhac_han.sql` và
`92_demo_bxh_vi_pham.sql` là dữ liệu kiểm thử tùy chọn.
Chỉ chạy ở DB thử sau khi cài xong 00–14.

## 5. Cấu hình MySQL

Sao chép db.properties.example thành db.properties tại thư mục gốc.

```properties
db.url=jdbc:mysql://localhost:3306/thu_vien_tu_dong?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=UTF-8
db.user=root
db.password=DIEN_MAT_KHAU_MYSQL
```

db.properties chứa thông tin kết nối trên máy chạy ứng dụng.
File này đã được đưa vào .gitignore.

Tài khoản mẫu trong script khởi tạo:

| Tài khoản | Mật khẩu mẫu | Vai trò |
|---|---|---|
| admin | 123456 | ADMIN |
| thuthu1 | 123456 | THU_THU |
| thuthu2 | 123456 | THU_THU |

Database đang dùng có thể đã được đổi mật khẩu riêng.

Mật khẩu mới được băm bằng PBKDF2. Khi người dùng đăng nhập
thành công bằng mật khẩu cũ chưa băm, app chuyển riêng mật khẩu
đó sang dạng băm và giữ nguyên mật khẩu người dùng nhập.
Bản app cũ không đọc được mật khẩu đã chuyển đổi. Hãy giữ bản
sao lưu và thử nâng cấp trên bản sao database trước.

## 6. Build và chạy

Tại thư mục gốc có pom.xml:

```powershell
mvn clean compile
mvn exec:java
```

Điểm khởi động chính là src/Main.java.

## 7. Quy ước Checkout

### Mã độc giả và mã phiếu

Mã độc giả mới hoặc mã được thay đổi phải là 12 chữ số CCCD.
Mã được lưu dưới dạng chuỗi để giữ số 0 ở đầu. Các hồ sơ cũ có
thể tiếp tục mượn, trả và chỉnh sửa thông tin bằng mã hiện có.
Chỉ nhập CCCD thật nếu thư viện có nhu cầu thu thập và quản lý
thông tin này. Khi demo hãy dùng mã thử. Biên nhận chỉ hiện bốn
số cuối nếu mã là CCCD.

Màn **Mượn & Trả sách** có hai tab **Mượn sách** và **Trả sách**.
Ở tab Trả sách, có thể tìm theo mã phiếu, CCCD/mã thẻ, tên độc giả
hoặc tên sách. Khi lập phiếu thành công, mã có dạng
`PM-YYYYMMDD-000123`, với ngày theo giờ Việt Nam và phần cuối là ID
phiếu (ít nhất sáu chữ số). Các phiếu cũ giữ nguyên mã.

### Bảng xếp hạng và dữ liệu demo

Chạy `database/92_demo_bxh_vi_pham.sql` trên database thử. Script
bổ sung 3 độc giả mẫu, 4 đầu sách và 12 phiếu. Mở **BXH sách**.
Mỗi cặp phiếu và đầu sách tính một lượt, còn số bản là tổng số lượng.
Trả sách không làm giảm số liệu mượn trong lịch sử.

| Sách mẫu | Lượt mượn | Số bản đã mượn |
|---|---:|---:|
| Lập trình Java | 5 | 6 |
| Cơ sở dữ liệu | 4 | 5 |
| Mạng máy tính | 2 | 2 |
| Hệ điều hành | 1 | 1 |

Có thể dùng phiếu `DEMO-BXH-01` để thử trả muộn và `DEMO-BXH-06`
để thử trả hỏng. Ngày mượn và hạn trả của mẫu được tính từ lần nạp
đầu tiên. Chạy lại script không khôi phục các phiếu đã thao tác.

### Trả theo cả dòng

Một lần xử lý trả áp dụng cho toàn bộ số bản của dòng được chọn.
Giao diện hiển thị số bản trước khi xác nhận.

Nếu cần xử lý từng bản với tình trạng khác nhau, thủ thư lập
các dòng mượn riêng, mỗi dòng một bản.

### Tồn kho

- so_luong_tong: số bản còn sử dụng được thuộc hệ thống,
  bao gồm cả bản đang được mượn.
- so_luong_con: số bản còn sử dụng được ở quầy, gồm cả các bản
  đang giữ cho lượt đặt trước. Lúc lập phiếu, Java dành số bản
  đang giữ cho đúng người nhận.

| Cách kết thúc dòng mượn | Thay đổi tồn kho |
|---|---|
| Trả bình thường | Cộng số bản vào so_luong_con |
| Làm hỏng | Không cộng so_luong_con; giảm so_luong_tong |
| Làm mất | Không cộng so_luong_con; giảm so_luong_tong |

Hỏng hoặc mất phải có ghi chú.

Java cập nhật ngày/tình trạng trả, vi phạm, số lần vi phạm và
tồn kho trong cùng giao dịch. Lỗi giữa chừng phải rollback.

Dòng đã xử lý bị chặn trả lại lần nữa.

### Trạng thái hạn trả

- Đang mượn, chưa quá hạn.
- Chưa trả nhưng đã quá hạn.
- Trả sớm.
- Trả đúng hạn.
- Đã trả muộn.

Quá hạn của dòng chưa trả được tính theo ngày đang xem,
không cần sửa trạng thái mỗi ngày trong database.

Với mất sách, ngay_tra là ngày chốt mất; giao diện phân biệt
việc chốt mất với trả sách thực tế.

### Số lần vi phạm

Số lần vi phạm được tính theo bản ghi vi phạm, không nhân
theo số bản trong dòng.

Ví dụ: một dòng vừa trả muộn vừa làm hỏng tạo hai vi phạm,
nên số lần vi phạm tăng hai.

Vi phạm trả muộn được ghi khi trả/chốt dòng. Đánh dấu vi phạm
đã xử lý không làm giảm số lần vi phạm lịch sử.

### Đặt trước

Mỗi lượt đặt tương ứng một bản sách. Khi có bản dùng được về kho,
hệ thống cấp cho lượt chờ theo thứ tự đặt và thông báo giờ nhận.
Trong thời gian giữ, bản đó chỉ có thể được mượn bởi độc giả đến lượt.
Đến giờ hết hạn, tác vụ nền giải phóng lượt giữ và cấp cho lượt tiếp theo.
Tác vụ chạy khi app mở. Thủ thư cũng có thể quét thủ công.

Khi lập phiếu hoặc trả sách, thay đổi tồn kho và trạng thái lượt
đặt liên quan được lưu cùng giao dịch. Email chờ xử lý được lưu vào
MySQL, sau đó luồng nền mới gửi. Trạng thái giữ sách và trạng thái
gửi thư được lưu riêng.

### Phiếu và phí

Biên nhận in lại đọc mã phiếu, thời điểm lập, độc giả, thủ thư
và phí đã lưu. Biên bản đã xử lý dùng số tiền và thông tin xử lý
đã ghi nhận. Dữ liệu cũ thiếu một thông tin thì hiển thị chưa có dữ liệu.

Màn Thống kê phí thư viện tách phí thuê sau voucher theo ngày lập
phiếu và bồi thường đã ghi nhận theo ngày xử lý. Phí thuê là số
tiền trên phiếu, ứng dụng chưa đối soát khoản thanh toán.

## 8. Nhắc hạn bằng email

Ứng dụng tìm các dòng:

- Chưa trả.
- Trạng thái DANG_MUON.
- Số lượng lớn hơn 0.
- Hạn trả đúng ngày mai theo giờ Việt Nam.

Lịch quét khởi động khi mở ứng dụng. Sau mỗi lượt hoàn tất,
lượt tiếp theo bắt đầu sau 15 phút.

Lịch thuộc toàn bộ ứng dụng, tiếp tục tồn tại khi đăng xuất.
Ứng dụng đóng thì không còn tác vụ nền.

Có thể quét thủ công trong tab Nhắc hạn email. Bộ lọc danh sách
cho phép xem nhiều khoảng hạn. Gửi nhắc hạn vẫn áp dụng mốc ngày mai.

### Cá nhân hóa bằng AI

Java lấy tối đa năm dòng trả gần nhất theo ngày trả,
loại dòng chốt mất sách. Cùng ngày dùng ID giảm dần để
tạo thứ tự ổn định.

Mỗi dòng được tính là một lượt.

| Lịch sử gần đây | Nhóm |
|---|---|
| Không có lượt trả | CHUA_CO_LICH_SU |
| Có 1–2 lượt | CHUA_RO_XU_HUONG |
| Có 3–5 lượt, ít nhất 60% muộn | THUONG_TRA_MUON |
| Có 3–5 lượt, ít nhất 60% sớm/đúng hạn | THUONG_DUNG_HAN |
| Còn lại | CHUA_RO_XU_HUONG |

Đây là quy tắc đơn giản cho bản BTL, dựa trên lịch sử gần đây.

AI chỉ nhận tên nhóm hành vi. Tên độc giả, email, tên sách,
hạn trả, số bản và lịch sử chi tiết không được gửi cho AI.

AI viết lời dẫn ngắn; Java ghép thông tin từ MySQL.
Nếu AI lỗi hoặc nội dung không hợp lệ, dùng mẫu cố định.

### Xem trước

Xem trước chỉ đọc dữ liệu, gọi AI để soạn bản nháp và hiển thị.

Không tạo hàng đợi, đổi trạng thái hoặc gọi SMTP.

AI được gọi lại khi gửi, nên cách diễn đạt có thể khác bản nháp.
Lịch sử lưu nội dung thực tế đã dùng để gửi.

### Chống gửi trùng

Mỗi cặp id_chi_tiet và moc_nhac có một bản ghi nhắc hạn.

Các trạng thái:

| Trạng thái | Ý nghĩa |
|---|---|
| CHUA_GUI | Chờ xử lý |
| DANG_GUI | Đã nhận xử lý |
| DA_GUI | SMTP đã chấp nhận email |
| LOI | Lỗi được xác định trước khi gửi |
| CAN_KIEM_TRA | Chưa xác định chắc chắn kết quả |
| HUY | Hủy trước SMTP vì không còn điều kiện |

Không tự gửi lại các trạng thái DA_GUI, DANG_GUI hoặc CAN_KIEM_TRA.

DA_GUI xác nhận SMTP chấp nhận, chưa bảo đảm thư đã vào hộp thư đến.

Ở REAL, phiên DANG_GUI cũ quá 10 phút được chuyển thành
CAN_KIEM_TRA. TEST bỏ qua cập nhật trạng thái hàng loạt,
nên phiên cũ có thể còn DANG_GUI để kiểm tra thủ công.

Dữ liệu dòng mượn được đọc lại sau khi chờ AI và trước khi gửi.
Nếu sách vừa được trả sau lần kiểm tra cuối trong lúc SMTP đang
xử lý, lời nhắc vẫn có thể đến.

## 9. Cấu hình gửi thử

Dùng Gmail có mật khẩu ứng dụng và một hộp thư do người kiểm thử
kiểm soát.

Trong PowerShell, điền địa chỉ Gmail gửi rồi chạy:

```powershell
$env:SMTP_MODE = "TEST"
$env:SMTP_TEST_READER_PREFIX = "BTL_MAIL_"

$env:SMTP_USER = "DIEN_GMAIL_GUI"
$env:SMTP_TEST_TO = $env:SMTP_USER

$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_SECURITY = "STARTTLS"
$env:SMTP_AUTH = "true"

$env:SMTP_FROM = $env:SMTP_USER
$env:SMTP_FROM_NAME = "Thu vien - BTL"

$env:AI_MODEL = "gemini-3.1-flash-lite"
```

Chạy riêng dòng nhập mật khẩu SMTP, nhập xong rồi nhấn Enter:

```powershell
$thuVienMkSmtp = Read-Host "Nhap mat khau ung dung Gmail" -AsSecureString
```

Khi prompt PowerShell trở lại:

```powershell
$env:SMTP_PASSWORD = [System.Net.NetworkCredential]::new("", $thuVienMkSmtp).Password
Remove-Variable thuVienMkSmtp
```

Nhập riêng khóa Gemini:

```powershell
$thuVienKhoaAi = Read-Host "Nhap Gemini API key" -AsSecureString
```

Khi prompt PowerShell trở lại:

```powershell
$env:AI_API_KEY = [System.Net.NetworkCredential]::new("", $thuVienKhoaAi).Password
Remove-Variable thuVienKhoaAi
```

Khởi động từ chính Terminal vừa cấu hình:

```powershell
mvn exec:java
```

Biến môi trường trong Terminal chỉ áp dụng cho tiến trình
được mở từ Terminal đó.

TEST chỉ quét mã thẻ có tiền tố được cấu hình và chuyển địa chỉ
nhận sang SMTP_TEST_TO. Thiếu tiền tố thì dừng quét trước khi
tạo email nhắc hạn hoặc gọi SMTP. Email đặt trước cũng kiểm tra
tiền tố trước khi gửi. Hai luồng dùng cùng quy tắc ghi nhận SMTP.

Dữ liệu thử email:
- Độc giả BTL_MAIL_DG.
- Sách BTL_MAIL_SACH.
- Một dòng mượn mới, chưa trả, hạn ngày mai.

Lịch quét chạy ngay khi app mở, có thể gửi trước lúc đăng nhập.

## 10. OOP và tổ chức code

| Nội dung | Áp dụng |
|---|---|
| Trừu tượng | Person |
| Kế thừa | NguoiDung và DocGia kế thừa Person |
| Ghi đè phương thức | getLoai(), getThongTinChiTiet() |
| Đóng gói | Thuộc tính và getter/setter trong model |
| Enum | NhomHanhVi |
| Ngoại lệ tùy chỉnh | DuLieuKhongHopLeException trong nghiệp vụ mượn–trả |
| Phân lớp | GUI, service, DAO, model |
| Giao dịch JDBC | Mượn/trả trong PhieuMuonDAO |
| Tác vụ nền | SwingWorker và ScheduledExecutorService |
| Singleton | KetNoiDatabase, ThongBaoService |

Các giao dịch mượn–trả và tác vụ nhắc hạn sử dụng connection riêng.
Không giữ connection truy vấn trong lúc chờ AI hoặc SMTP.

## 11. Kiểm thử

Nhóm lưu kết quả thực tế, ảnh chụp hoặc log của các ca:

| Ca | Kết quả mong đợi |
|---|---|
| Mượn vượt số bản còn | Bị từ chối, tồn kho không đổi |
| Trả sớm/đúng hạn/muộn | Lịch sử và số ngày đúng |
| Chưa trả đã quá hạn | Hiển thị riêng với đã trả muộn |
| Trả bình thường nhiều bản | Cộng đúng số bản của cả dòng |
| Hỏng/mất thiếu ghi chú | Bị từ chối |
| Trả muộn kèm hỏng/mất | Ghi đủ vi phạm, tăng đúng bộ đếm |
| Xử lý lại dòng đã trả | Bị từ chối, không cộng kho lần hai |
| Lỗi giữa giao dịch trả | Các thay đổi rollback |
| Form sách giữ tồn kho cũ | Bị chặn cập nhật ghi đè |
| Xem trước email nhiều lần | Không thêm thông báo hoặc gửi thư |
| Nhắc hạn ngày mai | Gửi đến hộp thư thử |
| AI lỗi | Dùng mẫu email cố định |
| Quét lại dòng đã gửi | Bỏ qua, không có thư trùng |
| Sách đã trả | Không còn đủ điều kiện nhắc hạn |
| Cài schema mới / chạy lại 12–14 | Cài được, giữ giao dịch cũ và không thêm cột trùng |
| Quyền admin cuối cùng | Không thể vô hiệu hóa hoặc hạ quyền |
| Bản được giữ cho độc giả khác | Phiếu mượn bị chặn nếu không còn bản trống |
| In lại phiếu mượn / phạt | Dùng thời điểm và số tiền của chứng từ gốc |
| Chuyển màn khi sửa sách hoặc độc giả | Giữ nội dung chưa lưu |

## 12. Giới hạn

- Tác vụ nhắc hạn chỉ chạy khi ứng dụng mở.
- Email hiện chỉ tự nhắc trước hạn một ngày.
- Vi phạm được xem trên app; chưa có email tự động riêng
  cho từng vi phạm hoặc sách đã quá hạn.
- Cấu hình MySQL bằng db.properties; SMTP/AI bằng biến môi trường.
- Sao lưu thực hiện thủ công qua MySQL Workbench.
- Các phiên gửi chưa rõ kết quả cần được kiểm tra trước khi xử lý tiếp.
