package dao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

final class MatKhauNguoiDung {
    private static final String TIEN_TO = "pbkdf2-sha256:";
    private static final int SO_VONG = 600_000;
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    private MatKhauNguoiDung() {}

    static boolean daBam(String giaTri) {
        return giaTri != null && giaTri.startsWith(TIEN_TO);
    }

    static String bam(String matKhau) {
        byte[] salt = new byte[16];
        NGAU_NHIEN.nextBytes(salt);
        byte[] khoa = taoKhoa(matKhau, salt, SO_VONG);
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            return TIEN_TO + SO_VONG + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(khoa);
        } finally {
            Arrays.fill(khoa, (byte) 0);
        }
    }

    static boolean dung(String matKhau, String giaTriDaLuu) {
        if (matKhau == null || giaTriDaLuu == null) {
            return false;
        }
        if (!daBam(giaTriDaLuu)) {
            return MessageDigest.isEqual(matKhau.getBytes(StandardCharsets.UTF_8),
                    giaTriDaLuu.getBytes(StandardCharsets.UTF_8));
        }

        String[] phan = giaTriDaLuu.split(":", -1);
        if (phan.length != 4) {
            return false;
        }
        try {
            int soVong = Integer.parseInt(phan[1]);
            byte[] salt = Base64.getDecoder().decode(phan[2]);
            byte[] daLuu = Base64.getDecoder().decode(phan[3]);
            if (soVong < 100_000 || soVong > 2_000_000 || salt.length != 16 || daLuu.length != 32) {
                return false;
            }
            byte[] doiChieu = taoKhoa(matKhau, salt, soVong);
            try {
                return MessageDigest.isEqual(doiChieu, daLuu);
            } finally {
                Arrays.fill(doiChieu, (byte) 0);
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] taoKhoa(String matKhau, byte[] salt, int soVong) {
        char[] kyTu = matKhau.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(kyTu, salt, soVong, 256);
        Arrays.fill(kyTu, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không thể kiểm tra mật khẩu.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
