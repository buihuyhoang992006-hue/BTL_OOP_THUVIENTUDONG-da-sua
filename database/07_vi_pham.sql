USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG VI_PHAM
-- ============================================================
CREATE TABLE IF NOT EXISTS vi_pham (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_vi_pham          VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia          INT NOT NULL,
    id_chi_tiet         INT,
    loai_vi_pham        ENUM(
                            'TRA_MUON',
                            'LAM_HONG',
                            'LAM_MAT'
                        ) NOT NULL,
    so_ngay_qua_han     INT NOT NULL DEFAULT 0,
    ngay_ghi_nhan       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mo_ta               TEXT,
    trang_thai          ENUM('CHUA_XU_LY', 'DA_XU_LY')
    NOT NULL DEFAULT 'CHUA_XU_LY',

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id),

    FOREIGN KEY (id_chi_tiet)
        REFERENCES chi_tiet_phieu_muon(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;