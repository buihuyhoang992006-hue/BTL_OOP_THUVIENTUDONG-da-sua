INSERT INTO thu_vien_tu_dong.doc_gia (
    ma_doc_gia,
    ho_ten,
    email,
    so_dien_thoai,
    dia_chi,
    diem_thuong,
    so_lan_vi_pham,
    trang_thai
)
SELECT
    'BTL_MAIL_DG',
    '[DEMO EMAIL] Độc giả kiểm thử',
    'btl.mail.demo@example.invalid',
    '0900000091',
    'Dữ liệu kiểm thử email',
    0,
    0,
    'HOAT_DONG'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.doc_gia
    WHERE ma_doc_gia = 'BTL_MAIL_DG'
);

INSERT INTO thu_vien_tu_dong.sach (
    ma_sach,
    ten_sach,
    tac_gia,
    nam_xuat_ban,
    so_luong_tong,
    so_luong_con,
    mo_ta,
    trang_thai
)
SELECT
    'BTL_MAIL_SACH',
    '[DEMO EMAIL] Sách kiểm thử nhắc hạn',
    'Nhóm BTL',
    2026,
    8,
    8,
    'Sách demo dùng để kiểm tra email nhắc hạn.',
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM thu_vien_tu_dong.sach
    WHERE ma_sach = 'BTL_MAIL_SACH'
);

SELECT
    DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR)
    + INTERVAL 1 DAY AS han_tra_demo;