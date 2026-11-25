USE thu_vien_tu_dong;

-- ============================================================
-- BẢNG CHI_TIET_PHIEU_MUON
-- ============================================================
CREATE TABLE IF NOT EXISTS chi_tiet_phieu_muon (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    id_phieu_muon       INT NOT NULL,
    id_sach             INT NOT NULL,
    so_luong            INT NOT NULL DEFAULT 1,
    han_tra             DATE NOT NULL,
    ngay_tra            DATE,
    trang_thai          ENUM(
                           'DANG_MUON',
                           'TRA_SOM',
                           'DUNG_HAN',
                           'QUA_HAN'
                        ) NOT NULL DEFAULT 'DANG_MUON',
    so_ngay_qua_han     INT NOT NULL DEFAULT 0,

    FOREIGN KEY (id_phieu_muon)
        REFERENCES phieu_muon(id)
        ON DELETE CASCADE,

    FOREIGN KEY (id_sach)
        REFERENCES sach(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
