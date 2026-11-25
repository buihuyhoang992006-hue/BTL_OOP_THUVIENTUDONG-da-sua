USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG PHIEU_MUON
-- ============================================================
CREATE TABLE IF NOT EXISTS phieu_muon (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_phieu            VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia          INT NOT NULL,
    id_thu_thu          INT,
    ngay_muon           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trang_thai          ENUM('DANG_MUON', 'DA_HOAN_THANH')
                        NOT NULL DEFAULT 'DANG_MUON',
    id_voucher          INT NULL DEFAULT NULL,
    ma_voucher_ap_dung  VARCHAR(30) NULL DEFAULT NULL,
    tong_tien_thue      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    giam_gia_voucher    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    thanh_tien          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    diem_cong           INT NOT NULL DEFAULT 0,

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id),

    FOREIGN KEY (id_thu_thu)
        REFERENCES nguoi_dung(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
