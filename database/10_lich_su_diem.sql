USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG LICH_SU_DIEM
-- ============================================================
CREATE TABLE IF NOT EXISTS lich_su_diem (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    ma_lich_su      VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia      INT NOT NULL,
    so_diem         INT NOT NULL,
    loai            ENUM(
                        'CONG',
                        'TRU'
                    ) NOT NULL,
    noi_dung        VARCHAR(255),
    ngay_tao        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;