USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG VOUCHER (GIỮ TƯƠNG THÍCH CŨ)
-- ============================================================
CREATE TABLE IF NOT EXISTS voucher (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_voucher          VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia          INT NOT NULL,
    phan_tram_giam      INT NOT NULL DEFAULT 5,
    ngay_tao            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    han_su_dung         DATE,
    trang_thai          ENUM(
                            'CHUA_SU_DUNG',
                            'DA_SU_DUNG',
                            'HET_HAN'
                        ) NOT NULL DEFAULT 'CHUA_SU_DUNG',

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- BẢNG VOUCHER_DOC_GIA (CHÍNH THỨC: TIỀN MẶT & PHẦN TRĂM)
-- ============================================================
CREATE TABLE IF NOT EXISTS voucher_doc_gia (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    ma_voucher      VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia      INT NOT NULL,
    ten_uu_dai      VARCHAR(150) NOT NULL,
    loai_giam       ENUM('TIEN_MAT', 'PHAN_TRAM') NOT NULL,
    gia_tri_giam    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    giam_toi_da     DECIMAL(12,2) NULL DEFAULT NULL,
    trang_thai      ENUM('CHUA_DUNG', 'DA_DUNG', 'HET_HAN') NOT NULL DEFAULT 'CHUA_DUNG',
    ngay_doi        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ngay_su_dung    DATETIME NULL DEFAULT NULL,

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;