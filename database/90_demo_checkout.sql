-- Chỉ bổ sung dữ liệu demo chưa tồn tại.
-- Chạy lại không đặt lại tồn kho, trạng thái hoặc bộ đếm cũ.

INSERT INTO thu_vien_tu_dong.sach (
    ma_sach, ten_sach, tac_gia, nam_xuat_ban,
    so_luong_tong, so_luong_con, mo_ta, trang_thai
)
SELECT
    'BTL_CK_SACH',
    '[DEMO] Sách kiểm thử Checkout',
    'Nhóm BTL',
    2026,
    20,
    20,
    'Dữ liệu thử mượn, trả, quá hạn, hỏng và mất.',
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.sach
    WHERE ma_sach = 'BTL_CK_SACH'
);

INSERT INTO thu_vien_tu_dong.sach (
    ma_sach, ten_sach, tac_gia, nam_xuat_ban,
    so_luong_tong, so_luong_con, mo_ta, trang_thai
)
SELECT
    'BTL_CK_CUOI',
    '[DEMO] Sách chỉ còn một bản',
    'Nhóm BTL',
    2026,
    1,
    1,
    'Dùng kiểm thử không đủ số lượng.',
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.sach
    WHERE ma_sach = 'BTL_CK_CUOI'
);

INSERT INTO thu_vien_tu_dong.doc_gia (
    ma_doc_gia, ho_ten, email, so_dien_thoai,
    dia_chi, diem_thuong, so_lan_vi_pham, trang_thai
)
SELECT
    'BTL_CK_DG',
    '[DEMO] Độc giả Checkout',
    'btl.checkout@example.invalid',
    '0900000001',
    'Dữ liệu kiểm thử',
    0,
    0,
    'HOAT_DONG'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.doc_gia
    WHERE ma_doc_gia = 'BTL_CK_DG'
);

INSERT INTO thu_vien_tu_dong.doc_gia (
    ma_doc_gia, ho_ten, email, so_dien_thoai,
    dia_chi, diem_thuong, so_lan_vi_pham, trang_thai
)
SELECT
    'BTL_CK_KHOA',
    '[DEMO] Độc giả bị khóa',
    'btl.checkout.locked@example.invalid',
    '0900000002',
    'Dữ liệu kiểm thử',
    0,
    0,
    'BI_KHOA'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.doc_gia
    WHERE ma_doc_gia = 'BTL_CK_KHOA'
);