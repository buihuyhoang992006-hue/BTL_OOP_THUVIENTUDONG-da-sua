package exception;

/**
 * Exception khi không tìm thấy sách
 * Áp dụng: Custom Exception, Throw/Throws
 */
public class SachKhongTonTaiException extends Exception {
    private String maSach;

    public SachKhongTonTaiException(String maSach) {
        super("Không tìm thấy sách với mã: " + maSach);
        this.maSach = maSach;
    }

    public SachKhongTonTaiException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMaSach() { return maSach; }
}
