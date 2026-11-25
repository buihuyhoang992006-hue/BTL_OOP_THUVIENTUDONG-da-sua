USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG DANH_MUC (Thể loại sách)
-- ============================================================
CREATE TABLE IF NOT EXISTS danh_muc (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    ten_danh_muc VARCHAR(100) NOT NULL,
    mo_ta       TEXT,
    ngay_tao    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- DỮ LIỆU MẪU - DANH MỤC
-- ============================================================
INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES
   ('Văn học trong nước',
    'Tác phẩm văn học của các tác giả Việt Nam'),

   ('Văn học nước ngoài',
    'Tác phẩm văn học dịch từ các quốc gia khác'),

   ('Kỹ năng sống',
    'Sách phát triển bản thân, kỹ năng mềm'),

   ('Khoa học - Công nghệ',
    'Sách về CNTT, lập trình và khoa học'),

   ('Thiếu nhi',
    'Sách dành cho trẻ em và thiếu niên'),

   ('Giáo khoa - Tham khảo',
    'Sách giáo khoa và tài liệu tham khảo'),

   ('Kinh tế - Quản trị',
    'Sách kinh doanh, quản lý và tài chính'),

   ('Lịch sử - Địa lý',
    'Sách lịch sử, địa lý và văn hóa');
