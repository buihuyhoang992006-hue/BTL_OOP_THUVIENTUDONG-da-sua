USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG DAT_TRUOC
-- ============================================================
CREATE TABLE IF NOT EXISTS dat_truoc (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_dat_truoc       VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia          INT NOT NULL,
    id_sach             INT NOT NULL,
    ngay_dat            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    han_nhan_sach       DATETIME,
    trang_thai          ENUM(
                            'DANG_CHO',
                            'DA_THONG_BAO',
                            'DA_NHAN',
                            'DA_HUY',
                            'HET_HAN'
                        ) NOT NULL DEFAULT 'DANG_CHO',

    INDEX idx_dat_truoc_fifo (id_sach, trang_thai, ngay_dat, id),

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id),

    FOREIGN KEY (id_sach)
        REFERENCES sach(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
