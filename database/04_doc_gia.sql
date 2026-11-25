USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG DOC_GIA
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_gia (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_doc_gia          VARCHAR(20) NOT NULL UNIQUE,
    ho_ten              VARCHAR(150) NOT NULL,
    email               VARCHAR(100),
    so_dien_thoai       VARCHAR(15),
    dia_chi             TEXT,
    diem_tich_luy       INT NOT NULL DEFAULT 0,
    diem_thuong         INT NOT NULL DEFAULT 0,
    so_lan_vi_pham      INT NOT NULL DEFAULT 0,
    trang_thai          ENUM('HOAT_DONG', 'BI_KHOA')
    NOT NULL DEFAULT 'HOAT_DONG',
    ngay_tao            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
