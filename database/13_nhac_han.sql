USE thu_vien_tu_dong;

-- Hàng đợi email cho nhắc hạn và đặt trước. Giữ nguyên dữ liệu đã lưu.
DROP PROCEDURE IF EXISTS btl_mig_13_them_cot;
DROP PROCEDURE IF EXISTS btl_mig_13_thong_bao;
DELIMITER $$
CREATE PROCEDURE btl_mig_13_them_cot(IN p_cot VARCHAR(64), IN p_kieu VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao' AND COLUMN_NAME = p_cot
    ) THEN
        SET @btl_ddl = CONCAT('ALTER TABLE thong_bao ADD COLUMN `', p_cot, '` ', p_kieu);
        PREPARE btl_stmt FROM @btl_ddl;
        EXECUTE btl_stmt;
        DEALLOCATE PREPARE btl_stmt;
    END IF;
END$$

CREATE PROCEDURE btl_mig_13_thong_bao()
BEGIN
    DECLARE v_enum TEXT;
    DECLARE v_cu TEXT;
    DECLARE v_loai TEXT;
    DECLARE v_loai_cu TEXT;

    CALL btl_mig_13_them_cot('id_chi_tiet', 'INT NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('id_dat_truoc', 'INT NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('moc_nhac', 'VARCHAR(30) NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('email_nhan', 'VARCHAR(254) NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('tieu_de', 'VARCHAR(255) NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('so_lan_thu', 'INT NOT NULL DEFAULT 0');
    CALL btl_mig_13_them_cot('lan_thu_cuoi', 'DATETIME NULL DEFAULT NULL');
    CALL btl_mig_13_them_cot('loi_gui', 'TEXT NULL');

    -- Nối trạng thái còn thiếu, giữ thứ tự và giá trị enum đã có.
    SELECT COLUMN_TYPE INTO v_enum FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao'
      AND COLUMN_NAME = 'trang_thai' AND DATA_TYPE = 'enum';
    SET v_cu = v_enum;
    IF LOCATE(QUOTE('CHUA_GUI'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('CHUA_GUI'), ')');
    END IF;
    IF LOCATE(QUOTE('DA_GUI'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('DA_GUI'), ')');
    END IF;
    IF LOCATE(QUOTE('DANG_GUI'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('DANG_GUI'), ')');
    END IF;
    IF LOCATE(QUOTE('LOI'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('LOI'), ')');
    END IF;
    IF LOCATE(QUOTE('CAN_KIEM_TRA'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('CAN_KIEM_TRA'), ')');
    END IF;
    IF LOCATE(QUOTE('HUY'), v_enum) = 0 THEN
        SET v_enum = CONCAT(LEFT(v_enum, LENGTH(v_enum) - 1), ',', QUOTE('HUY'), ')');
    END IF;
    IF v_enum <> v_cu THEN
        SET @btl_ddl = CONCAT('ALTER TABLE thong_bao MODIFY COLUMN trang_thai ', v_enum,
                              ' NOT NULL DEFAULT ', QUOTE('CHUA_GUI'));
        PREPARE btl_stmt FROM @btl_ddl;
        EXECUTE btl_stmt;
        DEALLOCATE PREPARE btl_stmt;
    END IF;

    -- Một số database trung gian dùng enum loại thông báo ngắn hơn.
    SELECT COLUMN_TYPE INTO v_loai FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao'
      AND COLUMN_NAME = 'loai_thong_bao' AND DATA_TYPE = 'enum';
    SET v_loai_cu = v_loai;
    IF LOCATE(QUOTE('SAP_DEN_HAN'), v_loai) = 0 THEN
        SET v_loai = CONCAT(LEFT(v_loai, LENGTH(v_loai) - 1), ',', QUOTE('SAP_DEN_HAN'), ')');
    END IF;
    IF LOCATE(QUOTE('QUA_HAN'), v_loai) = 0 THEN
        SET v_loai = CONCAT(LEFT(v_loai, LENGTH(v_loai) - 1), ',', QUOTE('QUA_HAN'), ')');
    END IF;
    IF LOCATE(QUOTE('DEN_LUOT_DAT_TRUOC'), v_loai) = 0 THEN
        SET v_loai = CONCAT(LEFT(v_loai, LENGTH(v_loai) - 1), ',', QUOTE('DEN_LUOT_DAT_TRUOC'), ')');
    END IF;
    IF v_loai <> v_loai_cu THEN
        SET @btl_ddl = CONCAT('ALTER TABLE thong_bao MODIFY COLUMN loai_thong_bao ', v_loai, ' NOT NULL');
        PREPARE btl_stmt FROM @btl_ddl;
        EXECUTE btl_stmt;
        DEALLOCATE PREPARE btl_stmt;
    END IF;

    -- Nhận diện ràng buộc bằng cột, kể cả khi bản cũ dùng tên khác.
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao' AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'id_chi_tiet,moc_nhac'
           AND COUNT(*) = 2 AND COUNT(SUB_PART) = 0
    ) THEN
        ALTER TABLE thong_bao ADD CONSTRAINT uq_thong_bao_dong_moc UNIQUE (id_chi_tiet, moc_nhac);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao' AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'id_dat_truoc,moc_nhac'
           AND COUNT(*) = 2 AND COUNT(SUB_PART) = 0
    ) THEN
        ALTER TABLE thong_bao ADD CONSTRAINT uq_thong_bao_dat_moc UNIQUE (id_dat_truoc, moc_nhac);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao'
          AND COLUMN_NAME = 'id_chi_tiet' AND REFERENCED_TABLE_SCHEMA = DATABASE()
          AND REFERENCED_TABLE_NAME = 'chi_tiet_phieu_muon' AND REFERENCED_COLUMN_NAME = 'id'
    ) THEN
        ALTER TABLE thong_bao ADD CONSTRAINT fk_tb_nhac_han_chi_tiet
            FOREIGN KEY (id_chi_tiet) REFERENCES chi_tiet_phieu_muon(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thong_bao'
          AND COLUMN_NAME = 'id_dat_truoc' AND REFERENCED_TABLE_SCHEMA = DATABASE()
          AND REFERENCED_TABLE_NAME = 'dat_truoc' AND REFERENCED_COLUMN_NAME = 'id'
    ) THEN
        ALTER TABLE thong_bao ADD CONSTRAINT fk_tb_dat_truoc
            FOREIGN KEY (id_dat_truoc) REFERENCES dat_truoc(id) ON DELETE SET NULL;
    END IF;
END$$
DELIMITER ;
CALL btl_mig_13_thong_bao();
DROP PROCEDURE btl_mig_13_thong_bao;
DROP PROCEDURE btl_mig_13_them_cot;
