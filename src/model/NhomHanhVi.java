package model;

public enum NhomHanhVi {

    CHUA_CO_LICH_SU(
            "Chưa có lịch sử trả sách"
    ),

    CHUA_RO_XU_HUONG(
            "Chưa rõ xu hướng từ lịch sử gần đây"
    ),

    THUONG_DUNG_HAN(
            "Thường trả sớm hoặc đúng hạn"
    ),

    THUONG_TRA_MUON(
            "Thường trả muộn"
    );

    private final String moTa;

    NhomHanhVi(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}