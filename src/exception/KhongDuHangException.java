package exception;

/**
 * Exception khi số lượng tồn kho không đủ
 */
public class KhongDuHangException extends Exception {
    private int soLuongYeuCau;
    private int soLuongTon;

    public KhongDuHangException(int soLuongYeuCau, int soLuongTon) {
        super("Số lượng tồn kho không đủ! Yêu cầu: " + soLuongYeuCau + ", Tồn kho: " + soLuongTon);
        this.soLuongYeuCau = soLuongYeuCau;
        this.soLuongTon = soLuongTon;
    }

    public int getSoLuongYeuCau() { return soLuongYeuCau; }
    public int getSoLuongTon() { return soLuongTon; }
}
