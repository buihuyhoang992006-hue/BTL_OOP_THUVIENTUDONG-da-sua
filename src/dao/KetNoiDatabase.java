package dao;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Đọc cấu hình MySQL và kiểm tra schema trước khi giao kết nối cho DAO. */
public final class KetNoiDatabase {

    private static final Path CAU_HINH = Path.of("db.properties");
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/thu_vien_tu_dong"
                    + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    private static final Map<String, List<String>> COT_BAT_BUOC = Map.ofEntries(
            Map.entry("danh_muc", List.of("id", "ten_danh_muc")),
            Map.entry("sach", List.of("id", "ma_sach", "ten_sach", "so_luong_tong",
                    "so_luong_con", "gia_thue", "trang_thai")),
            Map.entry("nguoi_dung", List.of("id", "ten_dang_nhap", "ho_ten", "mat_khau",
                    "vai_tro", "trang_thai")),
            Map.entry("doc_gia", List.of("id", "ma_doc_gia", "ho_ten", "email",
                    "diem_tich_luy", "diem_thuong", "so_lan_vi_pham", "trang_thai")),
            Map.entry("phieu_muon", List.of("id", "ma_phieu", "id_doc_gia", "id_thu_thu",
                    "ngay_muon", "trang_thai", "id_voucher", "ma_voucher_ap_dung",
                    "tong_tien_thue", "giam_gia_voucher", "thanh_tien", "diem_cong")),
            Map.entry("chi_tiet_phieu_muon", List.of("id", "id_phieu_muon", "id_sach",
                    "so_luong", "han_tra", "ngay_tra", "trang_thai", "so_ngay_qua_han",
                    "tinh_trang_tra", "ghi_chu_tra", "don_gia_thue")),
            Map.entry("vi_pham", List.of("id", "id_doc_gia", "id_chi_tiet", "loai_vi_pham",
                    "trang_thai", "tien_phat", "ngay_xu_ly", "diem_khoi_phuc", "ghi_chu_xu_ly")),
            Map.entry("dat_truoc", List.of("id", "ma_dat_truoc", "id_doc_gia", "id_sach",
                    "ngay_dat", "han_nhan_sach", "trang_thai")),
            Map.entry("thong_bao", List.of("id", "ma_thong_bao", "id_doc_gia",
                    "id_chi_tiet", "id_dat_truoc", "loai_thong_bao", "kenh_gui", "moc_nhac",
                    "noi_dung", "email_nhan", "tieu_de", "so_lan_thu", "lan_thu_cuoi",
                    "loi_gui", "ngay_tao", "ngay_gui", "trang_thai")),
            Map.entry("lich_su_diem", List.of("id", "ma_lich_su", "id_doc_gia", "so_diem",
                    "loai", "noi_dung", "ngay_tao")),
            Map.entry("voucher_doc_gia", List.of("id", "ma_voucher", "id_doc_gia", "ten_uu_dai",
                    "loai_giam", "gia_tri_giam", "giam_toi_da", "trang_thai", "ngay_doi", "ngay_su_dung"))
    );

    private final String url;
    private final String user;
    private final String password;
    private volatile boolean daKiemTraCauTruc;
    private Connection connection;
    private static KetNoiDatabase instance;

    private KetNoiDatabase() {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(CAU_HINH, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Không đọc được db.properties. Hãy sao chép db.properties.example và điền cấu hình MySQL.", ex);
        }
        url = props.getProperty("db.url", DEFAULT_URL).trim();
        user = props.getProperty("db.user", "root").trim();
        password = props.getProperty("db.password", "");
    }

    public static synchronized KetNoiDatabase getInstance() {
        if (instance == null) {
            instance = new KetNoiDatabase();
        }
        return instance;
    }

    /**
     * Giữ tương thích với DAO cũ dùng kết nối chung, các giao dịch mới tự sở hữu
     * và đóng kết nối được tạo bởi taoKetNoiChoGiaoDich.
     */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = moKetNoiDaKiemTra();
            }
            return connection;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex instanceof ThieuCauTrucException ? ex.getMessage()
                    : "Không thể kết nối MySQL. Kiểm tra db.properties và dịch vụ MySQL.", ex);
        }
    }

    public static synchronized void dongKetNoi() {
        if (instance != null) {
            instance.dongKetNoiChung();
            instance = null;
        }
    }

    private synchronized void dongKetNoiChung() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                System.err.println("Không thể đóng kết nối MySQL.");
            } finally {
                connection = null;
            }
        }
    }

    public static Connection taoKetNoiChoGiaoDich() throws SQLException {
        KetNoiDatabase db;
        try {
            db = getInstance();
        } catch (IllegalStateException ex) {
            throw new SQLNonTransientConnectionException(ex.getMessage(), "08001", ex);
        }
        return db.moKetNoiDaKiemTra();
    }

    private Connection moKetNoiDaKiemTra() throws SQLException {
        Connection conn = DriverManager.getConnection(url, user, password);
        try {
            try (Statement st = conn.createStatement()) {
                st.execute("SET SESSION time_zone = '+07:00'");
            }
            if (!daKiemTraCauTruc) {
                kiemTraCauTrucMotLan(conn);
            }
            return conn;
        } catch (SQLException | RuntimeException ex) {
            try {
                conn.close();
            } catch (SQLException closeError) {
                ex.addSuppressed(closeError);
            }
            throw ex;
        }
    }

    private synchronized void kiemTraCauTrucMotLan(Connection conn) throws SQLException {
        if (daKiemTraCauTruc) {
            return;
        }
        String catalog = conn.getCatalog();
        if (catalog == null || catalog.isBlank()) {
            throw new ThieuCauTrucException(List.of("chưa chọn database trong db.url"));
        }
        Map<String, Map<String, String>> cauTruc = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ?
                """)) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cauTruc.computeIfAbsent(rs.getString(1), k -> new HashMap<>())
                            .put(rs.getString(2), rs.getString(3));
                }
            }
        }

        List<String> thieu = new ArrayList<>();
        for (String bang : COT_BAT_BUOC.keySet().stream().sorted().toList()) {
            Map<String, String> cot = cauTruc.get(bang);
            if (cot == null) {
                thieu.add("bảng " + bang);
                continue;
            }
            for (String ten : COT_BAT_BUOC.get(bang)) {
                if (!cot.containsKey(ten)) {
                    thieu.add(bang + "." + ten);
                }
            }
        }
        Map<String, String> thongBao = cauTruc.get("thong_bao");
        if (thongBao != null) {
            kiemTraEnum(thieu, thongBao, "trang_thai", "CHUA_GUI", "DA_GUI", "DANG_GUI",
                    "LOI", "CAN_KIEM_TRA", "HUY");
            kiemTraEnum(thieu, thongBao, "loai_thong_bao", "SAP_DEN_HAN", "QUA_HAN", "DEN_LUOT_DAT_TRUOC");
            kiemTraKhoaEmail(conn, catalog, thieu);
        }
        if (!thieu.isEmpty()) {
            throw new ThieuCauTrucException(thieu);
        }
        daKiemTraCauTruc = true;
    }

    private static void kiemTraEnum(List<String> thieu, Map<String, String> cot,
                                     String tenCot, String... giaTri) {
        String dinhNghia = cot.get(tenCot);
        if (dinhNghia == null || !dinhNghia.startsWith("enum(")) {
            return;
        }
        for (String giaTriBatBuoc : giaTri) {
            if (!dinhNghia.contains("'" + giaTriBatBuoc + "'")) {
                thieu.add("thong_bao." + tenCot + " thiếu giá trị " + giaTriBatBuoc);
            }
        }
    }

    private static void kiemTraKhoaEmail(Connection conn, String catalog, List<String> thieu)
            throws SQLException {
        Set<String> khoa = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'thong_bao' AND NON_UNIQUE = 0
                GROUP BY INDEX_NAME
                HAVING COUNT(*) = 2 AND COUNT(SUB_PART) = 0
                """)) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    khoa.add(rs.getString(1));
                }
            }
        }
        for (String cot : List.of("id_chi_tiet", "id_dat_truoc")) {
            if (!khoa.contains(cot + ",moc_nhac")) {
                thieu.add("khóa duy nhất thong_bao(" + cot + ", moc_nhac)");
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        try (Connection conn = taoKetNoiChoGiaoDich()) {
            System.out.println("Đã kết nối MySQL và kiểm tra cấu trúc database " + conn.getCatalog() + ".");
        }
    }

    public static final class ThieuCauTrucException extends SQLException {
        private static final long serialVersionUID = 1L;

        private ThieuCauTrucException(List<String> thieu) {
            super("Database chưa đúng cấu trúc. Thiếu: " + String.join(", ", thieu)
                    + ". Hãy áp dụng các script SQL theo README rồi thử lại.", "42S22");
        }
    }
}
