USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG SACH
-- ============================================================
CREATE TABLE IF NOT EXISTS sach (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    ma_sach         VARCHAR(20)  NOT NULL UNIQUE,
    ten_sach        VARCHAR(255) NOT NULL,
    tac_gia         VARCHAR(150),
    nha_xuat_ban    VARCHAR(150),
    nam_xuat_ban    INT,
    so_luong_tong   INT NOT NULL DEFAULT 0,
    so_luong_con    INT NOT NULL DEFAULT 0,
    gia_thue        DECIMAL(12,2) NOT NULL DEFAULT 10000.00,
    id_danh_muc     INT,
    mo_ta           TEXT,
    hinh_anh        VARCHAR(255),
    trang_thai      TINYINT(1)   NOT NULL DEFAULT 1,  -- 1=đang sử dụng, 0=ngừng sử dụng
    ngay_tao        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- DỮ LIỆU MẪU - SÁCH (KÈM GIÁ THUÊ / LƯỢT)
-- ============================================================
INSERT IGNORE INTO sach (
    ma_sach,
    ten_sach,
    tac_gia,
    nha_xuat_ban,
    nam_xuat_ban,
    so_luong_tong,
    so_luong_con,
    gia_thue,
    id_danh_muc,
    mo_ta
) VALUES

-- =========================
-- 1. VĂN HỌC TRONG NƯỚC
-- =========================
('SACH0001', 'Cho Tôi Xin Một Vé Đi Tuổi Thơ',
 'Nguyễn Nhật Ánh', 'NXB Trẻ', 2022,
 45, 45, 10000.00, 1,
 'Tác phẩm nổi tiếng của Nguyễn Nhật Ánh'),

('SACH0002', 'Dế Mèn Phiêu Lưu Ký',
 'Tô Hoài', 'NXB Kim Đồng', 2022,
 60, 60, 12000.00, 1,
 'Tác phẩm văn học thiếu nhi kinh điển'),

('SACH0003', 'Tắt Đèn',
 'Ngô Tất Tố', 'NXB Văn Học', 2021,
 25, 25, 10000.00, 1,
 'Tác phẩm văn học hiện thực Việt Nam'),

('SACH0004', 'Số Đỏ',
 'Vũ Trọng Phụng', 'NXB Văn Học', 2021,
 20, 20, 12000.00, 1,
 'Tiểu thuyết trào phúng nổi tiếng'),

('SACH0005', 'Chí Phèo',
 'Nam Cao', 'NXB Văn Học', 2022,
 30, 30, 10000.00, 1,
 'Tác phẩm văn học hiện thực Việt Nam'),

('SACH0006', 'Mắt Biếc',
 'Nguyễn Nhật Ánh', 'NXB Trẻ', 2023,
 35, 35, 12000.00, 1,
 'Tiểu thuyết về tình yêu và tuổi trẻ'),


-- =========================
-- 2. VĂN HỌC NƯỚC NGOÀI
-- =========================
('SACH0007', 'Nhà Giả Kim',
 'Paulo Coelho', 'NXB Hội Nhà Văn', 2022,
 35, 35, 15000.00, 2,
 'Tiểu thuyết triết học nổi tiếng thế giới'),

('SACH0008', 'Ông Già Và Biển Cả',
 'Ernest Hemingway', 'NXB Văn Học', 2022,
 22, 22, 12000.00, 2,
 'Tác phẩm kinh điển của Ernest Hemingway'),

('SACH0009', 'Kiêu Hãnh Và Định Kiến',
 'Jane Austen', 'NXB Văn Học', 2021,
 18, 18, 15000.00, 2,
 'Tiểu thuyết văn học cổ điển Anh'),

('SACH0010', 'Cuốn Theo Chiều Gió',
 'Margaret Mitchell', 'NXB Văn Học', 2020,
 15, 15, 18000.00, 2,
 'Tiểu thuyết kinh điển của văn học Mỹ'),

('SACH0011', 'Không Gia Đình',
 'Hector Malot', 'NXB Văn Học', 2022,
 24, 24, 15000.00, 2,
 'Tiểu thuyết nổi tiếng dành cho nhiều thế hệ'),


-- =========================
-- 3. KỸ NĂNG SỐNG
-- =========================
('SACH0012', 'Đắc Nhân Tâm',
 'Dale Carnegie', 'NXB Tổng Hợp', 2023,
 50, 50, 15000.00, 3,
 'Sách về nghệ thuật giao tiếp và ứng xử'),

('SACH0013', 'Tư Duy Nhanh Và Chậm',
 'Daniel Kahneman', 'NXB Lao Động', 2023,
 30, 30, 18000.00, 3,
 'Khám phá cách con người tư duy và ra quyết định'),

('SACH0014', 'Mindset - Tư Duy Thành Công',
 'Carol S. Dweck', 'NXB Lao Động', 2023,
 20, 20, 15000.00, 3,
 'Sách về tư duy phát triển'),

('SACH0015', '7 Thói Quen Hiệu Quả',
 'Stephen R. Covey', 'NXB Tổng Hợp', 2022,
 28, 28, 15000.00, 3,
 'Những thói quen giúp phát triển bản thân'),

('SACH0016', 'Đời Ngắn Đừng Ngủ Dài',
 'Robin Sharma', 'NXB Trẻ', 2021,
 25, 25, 12000.00, 3,
 'Sách truyền cảm hứng và phát triển bản thân'),

('SACH0017', 'Atomic Habits',
 'James Clear', 'NXB Thế Giới', 2023,
 40, 40, 18000.00, 3,
 'Sách về xây dựng và duy trì thói quen tốt'),


-- =========================
-- 4. KHOA HỌC - CÔNG NGHỆ
-- =========================
('SACH0018', 'Lập Trình Java Căn Bản',
 'Nguyễn Văn A', 'NXB Giáo Dục', 2024,
 20, 20, 15000.00, 4,
 'Giáo trình Java dành cho sinh viên'),

('SACH0019', 'Python Cho Người Mới Bắt Đầu',
 'Trần Thị B', 'NXB Thông Tin', 2024,
 18, 18, 15000.00, 4,
 'Lập trình Python từ cơ bản'),

('SACH0020', 'Cấu Trúc Dữ Liệu Và Giải Thuật',
 'Nguyễn Đức Nghĩa', 'NXB Bách Khoa', 2023,
 30, 30, 18000.00, 4,
 'Kiến thức cấu trúc dữ liệu và thuật toán'),

('SACH0021', 'Cơ Sở Dữ Liệu',
 'Nguyễn Kim Anh', 'NXB Giáo Dục', 2023,
 25, 25, 15000.00, 4,
 'Giáo trình cơ sở dữ liệu'),

('SACH0022', 'Lập Trình Hướng Đối Tượng Với Java',
 'Nhiều tác giả', 'NXB Giáo Dục', 2024,
 35, 35, 18000.00, 4,
 'Giáo trình lập trình hướng đối tượng'),

('SACH0023', 'Clean Code',
 'Robert C. Martin', 'Prentice Hall', 2022,
 20, 20, 20000.00, 4,
 'Các nguyên tắc viết mã nguồn sạch'),

('SACH0024', 'Design Patterns',
 'Erich Gamma', 'Addison-Wesley', 2021,
 15, 15, 20000.00, 4,
 'Các mẫu thiết kế phần mềm phổ biến'),

('SACH0025', 'Computer Networks',
 'Andrew S. Tanenbaum', 'Pearson', 2022,
 18, 18, 20000.00, 4,
 'Kiến thức nền tảng về mạng máy tính'),

('SACH0026', 'Operating System Concepts',
 'Abraham Silberschatz', 'Wiley', 2022,
 20, 20, 20000.00, 4,
 'Kiến thức cơ bản về hệ điều hành'),

('SACH0027', 'Artificial Intelligence: A Modern Approach',
 'Stuart Russell', 'Pearson', 2023,
 12, 12, 20000.00, 4,
 'Giáo trình nhập môn trí tuệ nhân tạo'),


-- =========================
-- 5. THIẾU NHI
-- =========================
('SACH0028', 'Harry Potter Và Hòn Đá Phù Thủy',
 'J.K. Rowling', 'NXB Trẻ', 2023,
 25, 25, 18000.00, 5,
 'Tập đầu của bộ truyện Harry Potter'),

('SACH0029', 'Hoàng Tử Bé',
 'Antoine de Saint-Exupéry', 'NXB Kim Đồng', 2022,
 35, 35, 10000.00, 5,
 'Tác phẩm nổi tiếng dành cho thiếu nhi'),

('SACH0030', 'Alice Ở Xứ Sở Thần Tiên',
 'Lewis Carroll', 'NXB Kim Đồng', 2021,
 22, 22, 12000.00, 5,
 'Truyện phiêu lưu kỳ ảo dành cho thiếu nhi'),

('SACH0031', 'Charlie Và Nhà Máy Sôcôla',
 'Roald Dahl', 'NXB Kim Đồng', 2022,
 20, 20, 12000.00, 5,
 'Truyện thiếu nhi nổi tiếng'),

('SACH0032', 'Những Cuộc Phiêu Lưu Của Tom Sawyer',
 'Mark Twain', 'NXB Kim Đồng', 2021,
 18, 18, 12000.00, 5,
 'Tác phẩm phiêu lưu dành cho thiếu nhi'),


-- =========================
-- 6. GIÁO KHOA - THAM KHẢO
-- =========================
('SACH0033', 'Toán Giải Tích - Tập 1',
 'Nguyễn Đình Trí', 'NXB Giáo Dục', 2023,
 30, 30, 10000.00, 6,
 'Giáo trình giải tích dành cho sinh viên'),

('SACH0034', 'Đại Số Tuyến Tính',
 'Nguyễn Hữu Việt Hưng', 'NXB Giáo Dục', 2023,
 25, 25, 10000.00, 6,
 'Tài liệu đại số tuyến tính'),

('SACH0035', 'Xác Suất Thống Kê',
 'Đặng Hùng Thắng', 'NXB Giáo Dục', 2023,
 28, 28, 12000.00, 6,
 'Giáo trình xác suất và thống kê'),

('SACH0036', 'Tiếng Anh Chuyên Ngành Công Nghệ Thông Tin',
 'Nhiều tác giả', 'NXB Giáo Dục', 2024,
 20, 20, 12000.00, 6,
 'Tài liệu tiếng Anh chuyên ngành CNTT'),

('SACH0037', 'Kỹ Năng Viết Báo Cáo Khoa Học',
 'Nhiều tác giả', 'NXB Đại Học Quốc Gia', 2022,
 15, 15, 10000.00, 6,
 'Tài liệu tham khảo về viết báo cáo'),


-- =========================
-- 7. KINH TẾ - QUẢN TRỊ
-- =========================
('SACH0038', 'Kinh Tế Học Hài Hước',
 'Steven D. Levitt', 'NXB Trẻ', 2022,
 15, 15, 15000.00, 7,
 'Góc nhìn thú vị về kinh tế học'),

('SACH0039', 'Quốc Gia Khởi Nghiệp',
 'Dan Senor', 'NXB Thế Giới', 2022,
 18, 18, 18000.00, 7,
 'Những bài học về đổi mới và khởi nghiệp'),

('SACH0040', 'Cha Giàu Cha Nghèo',
 'Robert Kiyosaki', 'NXB Trẻ', 2023,
 30, 30, 15000.00, 7,
 'Kiến thức cơ bản về tài chính cá nhân'),

('SACH0041', 'Từ Tốt Đến Vĩ Đại',
 'Jim Collins', 'NXB Trẻ', 2022,
 20, 20, 18000.00, 7,
 'Sách về quản trị doanh nghiệp'),

('SACH0042', 'Khởi Nghiệp Tinh Gọn',
 'Eric Ries', 'NXB Công Thương', 2023,
 22, 22, 18000.00, 7,
 'Phương pháp xây dựng doanh nghiệp khởi nghiệp'),


-- =========================
-- 8. LỊCH SỬ - ĐỊA LÝ
-- =========================
('SACH0043', 'Lịch Sử Việt Nam Bằng Tranh',
 'Nhiều tác giả', 'NXB Trẻ', 2023,
 40, 40, 12000.00, 8,
 'Lịch sử Việt Nam qua hình ảnh'),

('SACH0044', 'Đại Việt Sử Ký Toàn Thư',
 'Nhiều tác giả', 'NXB Văn Học', 2022,
 15, 15, 18000.00, 8,
 'Tư liệu lịch sử Việt Nam'),

('SACH0045', 'Việt Nam Sử Lược',
 'Trần Trọng Kim', 'NXB Văn Học', 2022,
 20, 20, 15000.00, 8,
 'Tác phẩm tổng quan lịch sử Việt Nam'),

('SACH0046', 'Lược Sử Thế Giới',
 'E.H. Gombrich', 'NXB Tri Thức', 2022,
 18, 18, 18000.00, 8,
 'Tổng quan lịch sử thế giới'),

('SACH0047', 'Sapiens - Lược Sử Loài Người',
 'Yuval Noah Harari', 'NXB Tri Thức', 2023,
 30, 30, 20000.00, 8,
 'Khái quát quá trình phát triển của loài người'),

('SACH0048', 'Địa Lý Việt Nam',
 'Nhiều tác giả', 'NXB Giáo Dục', 2023,
 25, 25, 12000.00, 8,
 'Tài liệu tham khảo về địa lý Việt Nam');
