package exception;

/**
 * Exception khi dữ liệu nhập vào không hợp lệ
 */
public class DuLieuKhongHopLeException extends Exception {
    private String truong;

    public DuLieuKhongHopLeException(String truong, String chiTiet) {
        super("Dữ liệu không hợp lệ - " + truong + ": " + chiTiet);
        this.truong = truong;
    }

    public String getTruong() { return truong; }
}
