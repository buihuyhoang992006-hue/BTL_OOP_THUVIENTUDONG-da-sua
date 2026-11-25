USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG THONG_BAO
-- ============================================================
CREATE TABLE IF NOT EXISTS thong_bao (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_thong_bao        VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia          INT NOT NULL,
    id_chi_tiet         INT NULL DEFAULT NULL,
    id_dat_truoc        INT NULL DEFAULT NULL,
    loai_thong_bao      ENUM(
                            'SAP_DEN_HAN',
                            'QUA_HAN',
                            'DEN_LUOT_DAT_TRUOC',
                            'VOUCHER'
                        ) NOT NULL,
    noi_dung            TEXT NOT NULL,
    kenh_gui            ENUM(
                            'EMAIL',
                            'SYSTEM'
                        ) NOT NULL DEFAULT 'SYSTEM',
    moc_nhac            VARCHAR(30) NULL DEFAULT NULL,
    email_nhan          VARCHAR(254) NULL DEFAULT NULL,
    tieu_de             VARCHAR(255) NULL DEFAULT NULL,
    so_lan_thu          INT NOT NULL DEFAULT 0,
    lan_thu_cuoi        DATETIME NULL DEFAULT NULL,
    loi_gui             TEXT NULL,
    ngay_tao            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ngay_gui            DATETIME,
    trang_thai          ENUM(
                            'CHUA_GUI',
                            'DA_GUI',
                            'DANG_GUI',
                            'LOI',
                            'CAN_KIEM_TRA',
                            'HUY'
                        ) NOT NULL DEFAULT 'CHUA_GUI',

    CONSTRAINT uq_thong_bao_dong_moc
        UNIQUE (id_chi_tiet, moc_nhac),
    CONSTRAINT uq_thong_bao_dat_moc
        UNIQUE (id_dat_truoc, moc_nhac),

    FOREIGN KEY (id_doc_gia)
        REFERENCES doc_gia(id),

    CONSTRAINT fk_tb_nhac_han_chi_tiet FOREIGN KEY (id_chi_tiet)
        REFERENCES chi_tiet_phieu_muon(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_tb_dat_truoc FOREIGN KEY (id_dat_truoc)
        REFERENCES dat_truoc(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
