-- Chạy sau khi khởi tạo database theo README và áp dụng migration 14. Toàn bộ dữ liệu dưới đây là giả lập.
-- Ngày mượn và hạn trả tính theo ngày nạp lần đầu, theo giờ Việt Nam.
-- Dành cho database demo. Nếu mã mẫu bị trùng hoặc chỉ còn một phần bộ mẫu, giữ nguyên DB và báo lỗi.
USE thu_vien_tu_dong;

DROP PROCEDURE IF EXISTS nap_mau_bxh_92;
DELIMITER $$
CREATE PROCEDURE nap_mau_bxh_92()
nap_mau: BEGIN
    DECLARE ngay_goc DATE DEFAULT (DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR));
    DECLARE thu_thu, doc_gia_id, sach_id, phieu_id, chi_tiet_id INT;
    DECLARE so_phieu_mau INT;
    DECLARE so_thu_tu, so_ban, ngay_lap, han, ngay_tra_mau, tre, thuong INT;
    DECLARE ma_s VARCHAR(20) CHARACTER SET ascii;
    DECLARE cccd CHAR(12) CHARACTER SET ascii;
    DECLARE ma_p VARCHAR(30);
    DECLARE gia DECIMAL(12,2);
    DECLARE het BOOLEAN DEFAULT FALSE;
    -- Các cột cuối là độ lệch ngày so với hôm nạp mẫu. NULL là chưa trả.
    DECLARE ds CURSOR FOR
        SELECT 1, 'BXH92-JAVA', '000000920001', 1, -20, -6, NULL
        UNION ALL SELECT 2,  'BXH92-JAVA', '000000920002', 1, -4,  10, NULL
        UNION ALL SELECT 3,  'BXH92-JAVA', '000000920003', 2, -31,-17, -18
        UNION ALL SELECT 4,  'BXH92-JAVA', '000000920001', 1, -52,-38, -38
        UNION ALL SELECT 5,  'BXH92-JAVA', '000000920002', 1, -44,-30, -28
        UNION ALL SELECT 6,  'BXH92-CSDL', '000000920002', 1, -3,  11, NULL
        UNION ALL SELECT 7,  'BXH92-CSDL', '000000920003', 2, -23, -9, -11
        UNION ALL SELECT 8,  'BXH92-CSDL', '000000920001', 1, -40,-26, -26
        UNION ALL SELECT 9,  'BXH92-CSDL', '000000920001', 1, -12,  2, NULL
        UNION ALL SELECT 10, 'BXH92-MANG', '000000920003', 1, -2,  12, NULL
        UNION ALL SELECT 11, 'BXH92-MANG', '000000920002', 1, -33,-19, -19
        UNION ALL SELECT 12, 'BXH92-HDH',  '000000920003', 1, -14,  0, NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET het = TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SELECT COUNT(*) INTO so_phieu_mau FROM phieu_muon
    WHERE ma_phieu REGEXP '^DEMO-BXH-(0[1-9]|1[0-2])$';
    IF so_phieu_mau = 12 THEN
        COMMIT;
        LEAVE nap_mau;
    END IF;
    IF so_phieu_mau <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bộ mẫu BXH đã tồn tại một phần. Hãy dùng một database demo mới.';
    END IF;

    SELECT MIN(id) INTO thu_thu FROM nguoi_dung
    WHERE trang_thai = 1 AND vai_tro IN ('THU_THU', 'ADMIN');
    IF thu_thu IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cần có ít nhất một tài khoản thủ thư hoặc admin hoạt động.';
    END IF;

    -- INSERT thông thường: nếu trùng mã có sẵn, hủy toàn bộ lần nạp.
    INSERT INTO doc_gia (ma_doc_gia, ho_ten, email, so_dien_thoai, dia_chi) VALUES
        ('000000920001', '[Mẫu] An',   'bxh92.an@example.invalid',   '0900009201', '12 đường Sách, Hà Nội'),
        ('000000920002', '[Mẫu] Bình', 'bxh92.binh@example.invalid', '0900009202', '24 đường Sách, Hà Nội'),
        ('000000920003', '[Mẫu] Chi',  'bxh92.chi@example.invalid',  '0900009203', '36 đường Sách, Hà Nội');
    INSERT INTO sach (ma_sach, ten_sach, tac_gia, so_luong_tong, so_luong_con, gia_thue) VALUES
        ('BXH92-JAVA', '[Mẫu] Lập trình Java',  'Nhóm BTL', 10, 10, 12000),
        ('BXH92-CSDL', '[Mẫu] Cơ sở dữ liệu',   'Nhóm BTL', 10, 10, 10000),
        ('BXH92-MANG', '[Mẫu] Mạng máy tính',   'Nhóm BTL', 10, 10, 15000),
        ('BXH92-HDH',  '[Mẫu] Hệ điều hành',    'Nhóm BTL', 10, 10, 14000);

    OPEN ds;
    lap: LOOP
        FETCH ds INTO so_thu_tu, ma_s, cccd, so_ban, ngay_lap, han, ngay_tra_mau;
        IF het THEN LEAVE lap; END IF;
        SET ma_p = CONCAT('DEMO-BXH-', LPAD(so_thu_tu, 2, '0'));
        SET tre = GREATEST(0, IFNULL(ngay_tra_mau - han, 0));
        SET thuong = IF(ngay_tra_mau IS NOT NULL AND tre = 0, 5, 0);
        SELECT id INTO doc_gia_id FROM doc_gia WHERE ma_doc_gia = cccd;
        SELECT id, gia_thue INTO sach_id, gia FROM sach WHERE ma_sach = ma_s;

        INSERT INTO phieu_muon (ma_phieu, id_doc_gia, id_thu_thu, ngay_muon, trang_thai,
                                tong_tien_thue, thanh_tien, diem_cong)
        VALUES (ma_p, doc_gia_id, thu_thu, ngay_goc + INTERVAL ngay_lap DAY,
                IF(ngay_tra_mau IS NULL, 'DANG_MUON', 'DA_HOAN_THANH'),
                gia * so_ban, gia * so_ban, 10 * so_ban);
        SET phieu_id = LAST_INSERT_ID();
        INSERT INTO chi_tiet_phieu_muon (id_phieu_muon, id_sach, so_luong, han_tra, ngay_tra, trang_thai,
                                         tinh_trang_tra, so_ngay_qua_han, don_gia_thue)
        VALUES (phieu_id, sach_id, so_ban, ngay_goc + INTERVAL han DAY, ngay_goc + INTERVAL ngay_tra_mau DAY,
                CASE WHEN ngay_tra_mau IS NULL THEN 'DANG_MUON' WHEN tre > 0 THEN 'QUA_HAN'
                     WHEN ngay_tra_mau = han THEN 'DUNG_HAN' ELSE 'TRA_SOM' END,
                IF(ngay_tra_mau IS NULL, NULL, 'BINH_THUONG'), tre, gia);
        SET chi_tiet_id = LAST_INSERT_ID();

        INSERT INTO lich_su_diem (ma_lich_su, id_doc_gia, so_diem, loai, noi_dung, ngay_tao)
        VALUES (CONCAT(ma_p, '-M'), doc_gia_id, 10 * so_ban, 'CONG', CONCAT('Mượn sách · ', ma_p),
                ngay_goc + INTERVAL ngay_lap DAY);
        IF ngay_tra_mau IS NULL THEN
            UPDATE sach
            SET so_luong_con = so_luong_con - so_ban
            WHERE id = sach_id AND so_luong_con >= so_ban;
            IF ROW_COUNT() <> 1 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bộ mẫu mượn nhiều hơn tồn kho.';
            END IF;
        ELSEIF tre > 0 THEN
            INSERT INTO vi_pham (ma_vi_pham, id_doc_gia, id_chi_tiet, loai_vi_pham, so_ngay_qua_han,
                                 ngay_ghi_nhan, mo_ta, tien_phat)
            VALUES (CONCAT('VP-', ma_p), doc_gia_id, chi_tiet_id, 'TRA_MUON', tre,
                    ngay_goc + INTERVAL ngay_tra_mau DAY, 'Phiếu mẫu đã trả muộn', 0);
            INSERT INTO lich_su_diem (ma_lich_su, id_doc_gia, so_diem, loai, noi_dung, ngay_tao)
            VALUES (CONCAT(ma_p, '-V'), doc_gia_id, 2 * tre, 'TRU', CONCAT('Trả muộn · ', ma_p),
                    ngay_goc + INTERVAL ngay_tra_mau DAY);
        ELSE
            INSERT INTO lich_su_diem (ma_lich_su, id_doc_gia, so_diem, loai, noi_dung, ngay_tao)
            VALUES (CONCAT(ma_p, '-T'), doc_gia_id, thuong, 'CONG', CONCAT('Hoàn tất đúng hạn · ', ma_p),
                    ngay_goc + INTERVAL ngay_tra_mau DAY);
        END IF;
        UPDATE doc_gia SET diem_tich_luy = diem_tich_luy + 10 * so_ban + thuong - 2 * tre,
                           diem_thuong = diem_thuong + 10 * so_ban + thuong - 2 * tre,
                           so_lan_vi_pham = so_lan_vi_pham + IF(tre > 0, 1, 0)
        WHERE id = doc_gia_id;
    END LOOP;
    CLOSE ds;
    COMMIT;
END$$
DELIMITER ;
CALL nap_mau_bxh_92();
DROP PROCEDURE IF EXISTS nap_mau_bxh_92;

-- Danh sách phiếu để đối chiếu trực tiếp trong MySQL Workbench.
SELECT p.ma_phieu, dg.ho_ten, s.ten_sach, ct.so_luong, ct.han_tra, ct.ngay_tra, ct.trang_thai
FROM phieu_muon p JOIN doc_gia dg ON dg.id = p.id_doc_gia
JOIN chi_tiet_phieu_muon ct ON ct.id_phieu_muon = p.id
JOIN sach s ON s.id = ct.id_sach
WHERE p.ma_phieu REGEXP '^DEMO-BXH-(0[1-9]|1[0-2])$'
ORDER BY p.ma_phieu;
