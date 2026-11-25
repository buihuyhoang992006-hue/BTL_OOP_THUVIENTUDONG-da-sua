package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DAO truy vấn số liệu cho màn hình Dashboard thư viện.
 * Tên bảng/cột được viết theo database/00..11 của dự án.
 */
public class ThongKeDAO {

    private static final ZoneId MUI_GIO_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DINH_DANG_NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");

    /** Số bản sách mượn và trả trong một ngày cụ thể. */
    public record NgayMuonTra(LocalDate ngay, String nhanNgay, int soBanMuon, int soBanTra) {}

    /** Cơ cấu trạng thái bản sách trong kho và đang lưu hành. */
    public record CoCauKhoSach(int sanSang, int dangMuon, int quaHanChuaTra) {
        public int tong() {
            return sanSang + dangMuon + quaHanChuaTra;
        }
    }

    /** Gói toàn bộ số liệu KPI và biểu đồ cho màn hình Tổng quan. */
    public record DuLieuTongQuan(
            int tongSach,
            int tongDocGia,
            int soDangMuon,
            int soSapDenHan,
            int soQuaHan,
            int soDatTruoc,
            List<NgayMuonTra> xuHuong7Ngay,
            CoCauKhoSach coCauKho) {}

    /** Bộ lọc thời gian cho màn hình Thống kê doanh thu. */
    public enum PhamViThoiGian {
        HOM_NAY("Hôm nay"),
        BAY_NGAY("7 ngày qua"),
        THANG_NAY("Tháng này"),
        TAT_CA("Tất cả");

        private final String nhanHienThi;

        PhamViThoiGian(String nhanHienThi) {
            this.nhanHienThi = nhanHienThi;
        }

        public String nhanHienThi() {
            return nhanHienThi;
        }

        @Override
        public String toString() {
            return nhanHienThi;
        }
    }

    /** Một mốc trên biểu đồ phí thuê theo ngày lập phiếu. */
    public record MocDoanhThuBieuDo(String nhanMoc, long tongTienThueGoc, long phiSauGiam) {}

    /** Một dòng chi tiết phí thuê theo phiếu mượn. */
    public record DongGiaoDichDoanhThu(
            String maPhieu,
            LocalDateTime ngayLapPhieu,
            String docGiaHienThi,
            String maVoucher,
            long tienThueSach,
            long giamGiaVoucher,
            long phiSauGiam) {}

    /** Gói toàn bộ KPI, chuỗi biểu đồ và bảng chi tiết cho Thống kê doanh thu. */
    public record BaoCaoDoanhThu(
            long tongTienThueGoc,
            long tongGiamGiaVoucher,
            long phiSauGiam,
            long boiThuongDaGhiNhan,
            int soPhieuPhatSinh,
            int soPhieuDungVoucher,
            List<MocDoanhThuBieuDo> bieuDo,
            List<DongGiaoDichDoanhThu> giaoDich) {}

    private Connection layKetNoi() {
        return KetNoiDatabase.getInstance().getConnection();
    }

    private int layMotSo(String sql) {
        return layMotSo(layKetNoi(), sql);
    }

    private int layMotSo(Connection conn, String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lấy số liệu thống kê: " + e.getMessage(), e);
        }
    }

    /** Tổng số bản sách trong kho, không phải số đầu sách khác nhau. */
    public int getTongSach() {
        String sql = """
                SELECT COALESCE(SUM(so_luong_tong), 0)
                FROM sach
                WHERE trang_thai = 1
                """;
        return layMotSo(sql);
    }

    /** Số độc giả đang hoạt động. */
    public int getTongDocGia() {
        String sql = """
                SELECT COUNT(*)
                FROM doc_gia
                WHERE trang_thai = 'HOAT_DONG'
                """;
        return layMotSo(sql);
    }

    /** Số bản sách hiện chưa được trả, bao gồm cả bản đã quá hạn. */
    public int getSoDangMuon() {
        String sql = """
                SELECT COALESCE(SUM(so_luong), 0)
                FROM chi_tiet_phieu_muon
                WHERE ngay_tra IS NULL
                """;
        return layMotSo(sql);
    }

    /** Số bản sách có hạn trả từ hôm nay đến hết 3 ngày tới. */
    public int getSoSapDenHan() {
        String sql = """
                SELECT COALESCE(SUM(so_luong), 0)
                FROM chi_tiet_phieu_muon
                WHERE ngay_tra IS NULL
                  AND han_tra BETWEEN CURDATE()
                                  AND DATE_ADD(CURDATE(), INTERVAL 3 DAY)
                """;
        return layMotSo(sql);
    }

    /** Số bản sách chưa trả và đã quá hạn. */
    public int getSoQuaHan() {
        String sql = """
                SELECT COALESCE(SUM(so_luong), 0)
                FROM chi_tiet_phieu_muon
                WHERE ngay_tra IS NULL
                  AND han_tra < CURDATE()
                """;
        return layMotSo(sql);
    }

    /** Số yêu cầu đặt trước còn đang chờ hoặc đã được thông báo. */
    public int getSoDatTruoc() {
        String sql = """
                SELECT COUNT(*)
                FROM dat_truoc
                WHERE trang_thai IN ('DANG_CHO', 'DA_THONG_BAO')
                """;
        return layMotSo(sql);
    }

    /** Hạng theo số phiếu chứa đầu sách, tính trên toàn bộ lịch sử mượn. */
    public record HangSach(String maSach, String tenSach, long soLuot, long soBan) {}

    public List<HangSach> layBangXepHangSach() throws SQLException {
        String sql = """
                SELECT s.ma_sach, s.ten_sach,
                       COUNT(DISTINCT ct.id_phieu_muon) AS so_luot, SUM(ct.so_luong) AS so_ban
                FROM chi_tiet_phieu_muon ct
                JOIN sach s ON s.id = ct.id_sach
                WHERE ct.so_luong > 0
                GROUP BY s.id, s.ma_sach, s.ten_sach
                ORDER BY so_luot DESC, so_ban DESC, s.ma_sach ASC
                """;
        List<HangSach> danhSach = new ArrayList<>();
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                danhSach.add(new HangSach(rs.getString("ma_sach"), rs.getString("ten_sach"),
                        rs.getLong("so_luot"), rs.getLong("so_ban")));
            }
        }
        return danhSach;
    }

    /**
     * Top 5 sách theo tổng số bản đã được mượn.
     * Mỗi dòng gồm: STT, tên sách, tổng số bản đã mượn.
     */
    public Object[][] getTop5SachMuonNhieu() {
        String sql = """
                SELECT s.ten_sach,
                       COALESCE(SUM(ct.so_luong), 0) AS so_ban_da_muon
                FROM sach s
                JOIN chi_tiet_phieu_muon ct ON ct.id_sach = s.id
                GROUP BY s.id, s.ten_sach
                ORDER BY so_ban_da_muon DESC, s.ten_sach ASC
                LIMIT 5
                """;

        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = layKetNoi().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int stt = 1;
            while (rs.next()) {
                rows.add(new Object[]{
                        stt++,
                        rs.getString("ten_sach"),
                        rs.getInt("so_ban_da_muon")
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lấy Top 5 sách: " + e.getMessage(), e);
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * Số phiếu mượn theo tháng trong 6 tháng gần nhất.
     * Dùng LinkedHashMap để giữ thứ tự thời gian từ cũ đến mới.
     */
    public Map<String, Integer> getLuotMuonTheoThang() {
        String sql = """
                SELECT DATE_FORMAT(pm.ngay_muon, '%m/%Y') AS thang,
                       COUNT(DISTINCT pm.id) AS so_phieu
                FROM phieu_muon pm
                WHERE pm.ngay_muon >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH)
                GROUP BY YEAR(pm.ngay_muon), MONTH(pm.ngay_muon)
                ORDER BY YEAR(pm.ngay_muon), MONTH(pm.ngay_muon)
                """;

        Map<String, Integer> data = new LinkedHashMap<>();
        try (PreparedStatement ps = layKetNoi().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("thang"), rs.getInt("so_phieu"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lấy thống kê theo tháng: " + e.getMessage(), e);
        }
        return data;
    }

    /**
     * Thống kê xu hướng số bản sách mượn và trả theo từng ngày trong 7 ngày gần nhất.
     * Luôn trả về đủ 7 mốc ngày liên tiếp (từ 6 ngày trước đến hôm nay), an toàn khi offline.
     */
    public List<NgayMuonTra> getXuHuongMuonTra7Ngay() {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            return getXuHuongMuonTra7Ngay(conn);
        } catch (Exception e) {
            System.err.println("Không thể tải xu hướng mượn/trả 7 ngày: " + e.getMessage());
            return taoKhung7NgayTrong(LocalDate.now(MUI_GIO_VN));
        }
    }

    /** Overload tái sử dụng kết nối có sẵn cho thống kê 7 ngày gần nhất. */
    public List<NgayMuonTra> getXuHuongMuonTra7Ngay(Connection conn) {
        LocalDate homNay = LocalDate.now(MUI_GIO_VN);
        LocalDate tuNgay = homNay.minusDays(6);

        Map<LocalDate, int[]> mapTheoNgay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            mapTheoNgay.put(tuNgay.plusDays(i), new int[]{0, 0});
        }

        String sqlMuon = """
                SELECT DATE(pm.ngay_muon) AS ngay, COALESCE(SUM(ct.so_luong), 0) AS tong_muon
                FROM phieu_muon pm
                JOIN chi_tiet_phieu_muon ct ON ct.id_phieu_muon = pm.id
                WHERE pm.ngay_muon >= ? AND pm.ngay_muon < ?
                GROUP BY DATE(pm.ngay_muon)
                """;
        String sqlTra = """
                SELECT ct.ngay_tra AS ngay, COALESCE(SUM(ct.so_luong), 0) AS tong_tra
                FROM chi_tiet_phieu_muon ct
                WHERE ct.ngay_tra IS NOT NULL
                  AND ct.ngay_tra >= ? AND ct.ngay_tra <= ?
                  AND (ct.tinh_trang_tra IS NULL OR ct.tinh_trang_tra <> 'LAM_MAT')
                GROUP BY ct.ngay_tra
                """;

        try (PreparedStatement psMuon = conn.prepareStatement(sqlMuon)) {
            psMuon.setObject(1, tuNgay.atStartOfDay());
            psMuon.setObject(2, homNay.plusDays(1).atStartOfDay());
            try (ResultSet rs = psMuon.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("ngay");
                    if (sqlDate != null) {
                        int[] cap = mapTheoNgay.get(sqlDate.toLocalDate());
                        if (cap != null) {
                            cap[0] = Math.max(0, rs.getInt("tong_muon"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi truy vấn số liệu mượn 7 ngày: " + e.getMessage());
        }

        try (PreparedStatement psTra = conn.prepareStatement(sqlTra)) {
            psTra.setDate(1, java.sql.Date.valueOf(tuNgay));
            psTra.setDate(2, java.sql.Date.valueOf(homNay));
            try (ResultSet rs = psTra.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("ngay");
                    if (sqlDate != null) {
                        int[] cap = mapTheoNgay.get(sqlDate.toLocalDate());
                        if (cap != null) {
                            cap[1] = Math.max(0, rs.getInt("tong_tra"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi truy vấn số liệu trả 7 ngày: " + e.getMessage());
        }

        List<NgayMuonTra> ketQua = new ArrayList<>(7);
        for (Map.Entry<LocalDate, int[]> entry : mapTheoNgay.entrySet()) {
            LocalDate ngay = entry.getKey();
            int[] cap = entry.getValue();
            ketQua.add(new NgayMuonTra(ngay, ngay.format(DINH_DANG_NGAY_NGAN), cap[0], cap[1]));
        }
        return ketQua;
    }

    private static List<NgayMuonTra> taoKhung7NgayTrong(LocalDate homNay) {
        LocalDate tuNgay = homNay.minusDays(6);
        List<NgayMuonTra> list = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate ngay = tuNgay.plusDays(i);
            list.add(new NgayMuonTra(ngay, ngay.format(DINH_DANG_NGAY_NGAN), 0, 0));
        }
        return list;
    }

    /** Bản sách hiện tại, không cộng các bản đã loại khỏi kho vì hỏng hoặc mất. */
    public CoCauKhoSach getCoCauTrangThaiKhoSach() {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            return getCoCauTrangThaiKhoSach(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Không tải được cơ cấu kho sách.", e);
        }
    }

    public CoCauKhoSach getCoCauTrangThaiKhoSach(Connection conn) throws SQLException {
        String sql = """
                SELECT (SELECT COALESCE(SUM(so_luong_con), 0) FROM sach WHERE trang_thai = 1) AS san_sang,
                       COALESCE(SUM(CASE WHEN ct.han_tra >= ? THEN ct.so_luong ELSE 0 END), 0) AS dang_muon,
                       COALESCE(SUM(CASE WHEN ct.han_tra < ? THEN ct.so_luong ELSE 0 END), 0) AS qua_han
                FROM chi_tiet_phieu_muon ct
                JOIN sach s ON s.id = ct.id_sach AND s.trang_thai = 1
                WHERE ct.ngay_tra IS NULL
                """;
        LocalDate homNay = LocalDate.now(MUI_GIO_VN);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, homNay);
            ps.setObject(2, homNay);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new CoCauKhoSach(rs.getInt("san_sang"), rs.getInt("dang_muon"), rs.getInt("qua_han"));
            }
        }
    }

    /**
     * Truy vấn trọn bộ 6 chỉ số KPI và dữ liệu 2 biểu đồ trên một kết nối độc lập,
     * phù hợp chạy bất đồng bộ trong SwingWorker.
     */
    public DuLieuTongQuan layDuLieuTongQuan() {
        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            int tongSach = layMotSo(conn, """
                    SELECT COALESCE(SUM(so_luong_tong), 0)
                    FROM sach
                    WHERE trang_thai = 1
                    """);
            int tongDocGia = layMotSo(conn, """
                    SELECT COUNT(*)
                    FROM doc_gia
                    WHERE trang_thai = 'HOAT_DONG'
                    """);
            int soDangMuon = layMotSo(conn, """
                    SELECT COALESCE(SUM(so_luong), 0)
                    FROM chi_tiet_phieu_muon
                    WHERE ngay_tra IS NULL
                    """);
            int soSapDenHan = layMotSo(conn, """
                    SELECT COALESCE(SUM(so_luong), 0)
                    FROM chi_tiet_phieu_muon
                    WHERE ngay_tra IS NULL
                      AND han_tra BETWEEN CURDATE()
                                      AND DATE_ADD(CURDATE(), INTERVAL 3 DAY)
                    """);
            int soQuaHan = layMotSo(conn, """
                    SELECT COALESCE(SUM(so_luong), 0)
                    FROM chi_tiet_phieu_muon
                    WHERE ngay_tra IS NULL
                      AND han_tra < CURDATE()
                    """);
            int soDatTruoc = layMotSo(conn, """
                    SELECT COUNT(*)
                    FROM dat_truoc
                    WHERE trang_thai IN ('DANG_CHO', 'DA_THONG_BAO')
                    """);
            List<NgayMuonTra> xuHuong7Ngay = getXuHuongMuonTra7Ngay(conn);
            CoCauKhoSach coCauKho = getCoCauTrangThaiKhoSach(conn);
            return new DuLieuTongQuan(
                    tongSach, tongDocGia, soDangMuon, soSapDenHan, soQuaHan, soDatTruoc,
                    xuHuong7Ngay, coCauKho);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải dữ liệu tổng quan: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // THỐNG KÊ DOANH THU (PHÍ THUÊ SÁCH & GIẢM GIÁ VOUCHER)
    // =========================================================================

    private static final DateTimeFormatter DINH_DANG_THANG = DateTimeFormatter.ofPattern("MM/yyyy");

    public BaoCaoDoanhThu layBaoCaoDoanhThu(PhamViThoiGian phamVi) {
        PhamViThoiGian boLoc = phamVi == null ? PhamViThoiGian.TAT_CA : phamVi;
        LocalDate homNay = LocalDate.now(MUI_GIO_VN);

        LocalDateTime tuLuc = switch (boLoc) {
            case HOM_NAY -> homNay.atStartOfDay();
            case BAY_NGAY -> homNay.minusDays(6).atStartOfDay();
            case THANG_NAY -> homNay.withDayOfMonth(1).atStartOfDay();
            case TAT_CA -> null;
        };
        LocalDateTime denLuc = boLoc == PhamViThoiGian.TAT_CA ? null : homNay.plusDays(1).atStartOfDay();

        // Chuẩn bị khung biểu đồ theo mốc thời gian đã chọn [tongTienThueGoc, phiSauGiam]
        Map<String, long[]> khungNgayHoacGio = new LinkedHashMap<>();
        TreeMap<YearMonth, long[]> khungThang = new TreeMap<>();

        if (boLoc == PhamViThoiGian.HOM_NAY) {
            khungNgayHoacGio.put("00h - 06h", new long[]{0L, 0L});
            khungNgayHoacGio.put("06h - 12h", new long[]{0L, 0L});
            khungNgayHoacGio.put("12h - 18h", new long[]{0L, 0L});
            khungNgayHoacGio.put("18h - 24h", new long[]{0L, 0L});
        } else if (boLoc == PhamViThoiGian.BAY_NGAY) {
            LocalDate start = homNay.minusDays(6);
            for (int i = 0; i < 7; i++) {
                khungNgayHoacGio.put(start.plusDays(i).format(DINH_DANG_NGAY_NGAN), new long[]{0L, 0L});
            }
        } else if (boLoc == PhamViThoiGian.THANG_NAY) {
            LocalDate dauThang = homNay.withDayOfMonth(1);
            int soNgay = homNay.getDayOfMonth();
            for (int i = 0; i < soNgay; i++) {
                khungNgayHoacGio.put(dauThang.plusDays(i).format(DINH_DANG_NGAY_NGAN), new long[]{0L, 0L});
            }
        } else {
            YearMonth ymNow = YearMonth.from(homNay);
            for (int i = 5; i >= 0; i--) {
                khungThang.put(ymNow.minusMonths(i), new long[]{0L, 0L});
            }
        }

        List<DongGiaoDichDoanhThu> rows = new ArrayList<>();
        long tongTienThueGoc = 0L;
        long tongGiamGiaVoucher = 0L;
        long phiSauGiam = 0L;
        int soPhieuCoDoanhThu = 0;
        int soPhieuDungVoucher = 0;
        long boiThuongDaGhiNhan;

        try (Connection conn = KetNoiDatabase.taoKetNoiChoGiaoDich()) {
            StringBuilder sqlMuon = new StringBuilder("""
                    SELECT pm.ma_phieu, pm.ngay_muon, pm.ma_voucher_ap_dung,
                           dg.ma_doc_gia, dg.ho_ten,
                           pm.tong_tien_thue, pm.giam_gia_voucher, pm.thanh_tien
                    FROM phieu_muon pm
                    JOIN doc_gia dg ON pm.id_doc_gia = dg.id
                    """);
            if (tuLuc != null) {
                sqlMuon.append("WHERE pm.ngay_muon >= ? AND pm.ngay_muon < ? ");
            }
            sqlMuon.append("ORDER BY pm.ngay_muon DESC, pm.id DESC");

            try (PreparedStatement ps = conn.prepareStatement(sqlMuon.toString())) {
                if (tuLuc != null) {
                    ps.setObject(1, tuLuc);
                    ps.setObject(2, denLuc);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String maPhieu = rs.getString("ma_phieu");
                        LocalDateTime ngayMuon = rs.getObject("ngay_muon", LocalDateTime.class);
                        String maVoucher = rs.getString("ma_voucher_ap_dung");
                        String docGia = dinhDangDocGiaDoanhThu(rs.getString("ma_doc_gia"), rs.getString("ho_ten"));
                        long tienThue = Math.max(0L, Math.round(rs.getDouble("tong_tien_thue")));
                        long giamGia = Math.max(0L, Math.round(rs.getDouble("giam_gia_voucher")));
                        long thanhTien = Math.max(0L, Math.round(rs.getDouble("thanh_tien")));

                        tongTienThueGoc += tienThue;
                        tongGiamGiaVoucher += giamGia;
                        phiSauGiam += thanhTien;
                        if (tienThue > 0 || thanhTien > 0) {
                            soPhieuCoDoanhThu++;
                        }
                        if (giamGia > 0 || (maVoucher != null && !maVoucher.isBlank())) {
                            soPhieuDungVoucher++;
                        }

                        if (tienThue > 0 || thanhTien > 0) {
                            congVaoKhungBieuDo(boLoc, khungNgayHoacGio, khungThang, ngayMuon, tienThue, thanhTien);
                            rows.add(new DongGiaoDichDoanhThu(
                                    maPhieu,
                                    ngayMuon,
                                    docGia,
                                    maVoucher == null ? "" : maVoucher,
                                    tienThue,
                                    giamGia,
                                    thanhTien));
                        }
                    }
                }
            }
            boiThuongDaGhiNhan = tongBoiThuongDaGhiNhan(conn, tuLuc, denLuc);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải thống kê phí thư viện.", e);
        }

        List<MocDoanhThuBieuDo> bieuDo = new ArrayList<>();
        if (boLoc == PhamViThoiGian.TAT_CA) {
            for (Map.Entry<YearMonth, long[]> entry : khungThang.entrySet()) {
                long[] val = entry.getValue();
                bieuDo.add(new MocDoanhThuBieuDo(entry.getKey().format(DINH_DANG_THANG), val[0], val[1]));
            }
        } else {
            for (Map.Entry<String, long[]> entry : khungNgayHoacGio.entrySet()) {
                long[] val = entry.getValue();
                bieuDo.add(new MocDoanhThuBieuDo(entry.getKey(), val[0], val[1]));
            }
        }

        return new BaoCaoDoanhThu(
                tongTienThueGoc,
                tongGiamGiaVoucher,
                phiSauGiam,
                boiThuongDaGhiNhan,
                soPhieuCoDoanhThu,
                soPhieuDungVoucher,
                bieuDo,
                rows);
    }

    private long tongBoiThuongDaGhiNhan(Connection conn, LocalDateTime tuLuc, LocalDateTime denLuc)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(tien_phat), 0)
                FROM vi_pham
                WHERE trang_thai = 'DA_XU_LY'
                  AND ngay_xu_ly IS NOT NULL
                  AND loai_vi_pham IN ('LAM_HONG', 'LAM_MAT')
                """;
        if (tuLuc != null) {
            sql += "AND ngay_xu_ly >= ? AND ngay_xu_ly < ?";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (tuLuc != null) {
                ps.setObject(1, tuLuc);
                ps.setObject(2, denLuc);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Math.round(rs.getDouble(1));
            }
        }
    }

    private static String dinhDangDocGiaDoanhThu(String maDocGia, String hoTen) {
        String ten = hoTen == null ? "" : hoTen.trim();
        String ma = maDocGia == null ? "" : maDocGia.trim();
        if (ma.isEmpty()) {
            return ten;
        }
        return ma + " - " + ten;
    }

    private static void congVaoKhungBieuDo(
            PhamViThoiGian boLoc,
            Map<String, long[]> khungNgayHoacGio,
            TreeMap<YearMonth, long[]> khungThang,
            LocalDateTime thoiGian,
            long tienThueGoc,
            long phiSauGiam) {
        if (thoiGian == null) {
            return;
        }
        if (boLoc == PhamViThoiGian.TAT_CA) {
            YearMonth ym = YearMonth.from(thoiGian);
            long[] bucket = khungThang.computeIfAbsent(ym, k -> new long[]{0L, 0L});
            bucket[0] += tienThueGoc;
            bucket[1] += phiSauGiam;
            return;
        }

        String key;
        if (boLoc == PhamViThoiGian.HOM_NAY) {
            int h = thoiGian.getHour();
            if (h < 6) {
                key = "00h - 06h";
            } else if (h < 12) {
                key = "06h - 12h";
            } else if (h < 18) {
                key = "12h - 18h";
            } else {
                key = "18h - 24h";
            }
        } else {
            key = thoiGian.toLocalDate().format(DINH_DANG_NGAY_NGAN);
        }

        long[] bucket = khungNgayHoacGio.get(key);
        if (bucket != null) {
            bucket[0] += tienThueGoc;
            bucket[1] += phiSauGiam;
        }
    }
}
