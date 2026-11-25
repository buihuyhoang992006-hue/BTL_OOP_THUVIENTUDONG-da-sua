USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG NGUOI_DUNG
-- ============================================================
CREATE TABLE IF NOT EXISTS nguoi_dung (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    ho_ten          VARCHAR(150) NOT NULL,
    so_dien_thoai   VARCHAR(15),
    email           VARCHAR(100),
    dia_chi         TEXT,
    ten_dang_nhap   VARCHAR(50)  NOT NULL UNIQUE,
    mat_khau        VARCHAR(255) NOT NULL,
    vai_tro ENUM('ADMIN','THU_THU') NOT NULL DEFAULT 'THU_THU',
    trang_thai      TINYINT(1)   NOT NULL DEFAULT 1,
    ngay_tao        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- DỮ LIỆU MẪU - NGƯỜI DÙNG
-- Mật khẩu mẫu: 123456
-- ============================================================
INSERT INTO nguoi_dung (
    ho_ten,
    so_dien_thoai,
    email,
    dia_chi,
    ten_dang_nhap,
    mat_khau,
    vai_tro
) VALUES
      ('Quản Trị Viên',
       '0912345678',
       'admin@thuvien.vn',
       'Hà Nội',
       'admin',
       '123456',
       'ADMIN'),

      ('Nguyễn Thị Hương',
       '0987654321',
       'huong@thuvien.vn',
       'Cầu Giấy, Hà Nội',
       'thuthu1',
       '123456',
       'THU_THU'),

      ('Trần Văn Minh',
       '0976543210',
       'minh@thuvien.vn',
       'Đống Đa, Hà Nội',
       'thuthu2',
       '123456',
       'THU_THU');
