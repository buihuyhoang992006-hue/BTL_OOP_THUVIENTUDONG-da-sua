USE thu_vien_tu_dong;

-- Bổ sung dữ liệu trả sách còn thiếu. Có thể chạy lại sau khi sao lưu.
DROP PROCEDURE IF EXISTS btl_mig_12_checkout;
DELIMITER $$
CREATE PROCEDURE btl_mig_12_checkout()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chi_tiet_phieu_muon'
          AND COLUMN_NAME = 'tinh_trang_tra'
    ) THEN
        ALTER TABLE chi_tiet_phieu_muon
            ADD COLUMN tinh_trang_tra ENUM('BINH_THUONG', 'LAM_HONG', 'LAM_MAT') NULL DEFAULT NULL;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chi_tiet_phieu_muon'
          AND COLUMN_NAME = 'ghi_chu_tra'
    ) THEN
        ALTER TABLE chi_tiet_phieu_muon ADD COLUMN ghi_chu_tra TEXT NULL;
    END IF;
END$$
DELIMITER ;
CALL btl_mig_12_checkout();
DROP PROCEDURE btl_mig_12_checkout;
