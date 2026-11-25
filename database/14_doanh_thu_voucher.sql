USE thu_vien_tu_dong;

-- Chạy sau 12 và 13. Giữ nguyên giá, thành tiền, vi phạm và ngày xử lý đã lưu.
-- Cột mới chưa có dữ liệu sử dụng giá trị mặc định. Không tính lại giao dịch cũ.
DROP PROCEDURE IF EXISTS btl_mig_14_them_cot;
DROP PROCEDURE IF EXISTS btl_mig_14_doanh_thu;
DELIMITER $$
CREATE PROCEDURE btl_mig_14_them_cot(
    IN p_bang VARCHAR(64), IN p_cot VARCHAR(64), IN p_kieu VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_bang AND COLUMN_NAME = p_cot
    ) THEN
        SET @btl_ddl = CONCAT('ALTER TABLE `', p_bang, '` ADD COLUMN `', p_cot, '` ', p_kieu);
        PREPARE btl_stmt FROM @btl_ddl;
        EXECUTE btl_stmt;
        DEALLOCATE PREPARE btl_stmt;
    END IF;
END$$

CREATE PROCEDURE btl_mig_14_doanh_thu()
BEGIN
    CALL btl_mig_14_them_cot('sach', 'gia_thue', 'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
    CALL btl_mig_14_them_cot('doc_gia', 'diem_tich_luy', 'INT NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('doc_gia', 'diem_thuong', 'INT NULL DEFAULT NULL');

    -- Chuyển đúng số điểm cũ chỉ khi cột đích chưa từng có giá trị.
    -- Cho phép chạy lại nếu lần chạy đầu dừng giữa lúc thêm cột và sao chép.
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'doc_gia'
          AND COLUMN_NAME = 'diem_tich_luy' AND IS_NULLABLE = 'YES'
    ) THEN
        UPDATE doc_gia SET diem_tich_luy = COALESCE(diem_thuong, 0)
        WHERE id > 0 AND diem_tich_luy IS NULL;
        ALTER TABLE doc_gia MODIFY COLUMN diem_tich_luy INT NOT NULL DEFAULT 0;
    END IF;
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'doc_gia'
          AND COLUMN_NAME = 'diem_thuong' AND IS_NULLABLE = 'YES'
    ) THEN
        UPDATE doc_gia SET diem_thuong = diem_tich_luy WHERE id > 0 AND diem_thuong IS NULL;
        ALTER TABLE doc_gia MODIFY COLUMN diem_thuong INT NOT NULL DEFAULT 0;
    END IF;

    CALL btl_mig_14_them_cot('chi_tiet_phieu_muon', 'don_gia_thue', 'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
    CALL btl_mig_14_them_cot('phieu_muon', 'id_voucher', 'INT NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('phieu_muon', 'ma_voucher_ap_dung', 'VARCHAR(30) NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('phieu_muon', 'tong_tien_thue', 'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
    CALL btl_mig_14_them_cot('phieu_muon', 'giam_gia_voucher', 'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
    CALL btl_mig_14_them_cot('phieu_muon', 'thanh_tien', 'DECIMAL(12,2) NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('phieu_muon', 'diem_cong', 'INT NOT NULL DEFAULT 0');

    -- Nếu một bản cũ có cột thuc_thu, sao chép số đã lưu vào ô mới còn trống.
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'phieu_muon'
          AND COLUMN_NAME = 'thanh_tien' AND IS_NULLABLE = 'YES'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'phieu_muon' AND COLUMN_NAME = 'thuc_thu'
        ) THEN
            UPDATE phieu_muon SET thanh_tien = COALESCE(thuc_thu, 0.00)
            WHERE id > 0 AND thanh_tien IS NULL;
        ELSE
            UPDATE phieu_muon SET thanh_tien = 0.00 WHERE id > 0 AND thanh_tien IS NULL;
        END IF;
        ALTER TABLE phieu_muon MODIFY COLUMN thanh_tien DECIMAL(12,2) NOT NULL DEFAULT 0.00;
    END IF;

    CALL btl_mig_14_them_cot('vi_pham', 'tien_phat', 'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
    CALL btl_mig_14_them_cot('vi_pham', 'ngay_xu_ly', 'DATETIME NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('vi_pham', 'diem_khoi_phuc', 'INT NULL DEFAULT NULL');
    CALL btl_mig_14_them_cot('vi_pham', 'ghi_chu_xu_ly', 'TEXT NULL');

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dat_truoc'
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'id_sach,trang_thai,ngay_dat,id'
           AND COUNT(*) = 4 AND COUNT(SUB_PART) = 0
    ) THEN
        ALTER TABLE dat_truoc ADD INDEX idx_dat_truoc_fifo (id_sach, trang_thai, ngay_dat, id);
    END IF;
END$$
DELIMITER ;
CALL btl_mig_14_doanh_thu();
DROP PROCEDURE btl_mig_14_doanh_thu;
DROP PROCEDURE btl_mig_14_them_cot;

-- Các bản cũ có thể chưa có các bảng dưới đây.
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
    FOREIGN KEY (id_doc_gia) REFERENCES doc_gia(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lich_su_diem (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    ma_lich_su      VARCHAR(30) NOT NULL UNIQUE,
    id_doc_gia      INT NOT NULL,
    so_diem         INT NOT NULL,
    loai            ENUM('CONG', 'TRU') NOT NULL,
    noi_dung        VARCHAR(255),
    ngay_tao        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_doc_gia) REFERENCES doc_gia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
