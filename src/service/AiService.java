package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AiService {

    private static final String DIA_CHI_API =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final Gson JSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public static class AiException extends Exception {
        public AiException(String thongDiep) {
            super(thongDiep);
        }
    }

    private record CauHinh(String khoa, String model) {
    }

    public AiService() {
        // Không gọi API trong constructor.
    }

    /*
     * Chỉ kiểm tra biến môi trường, chưa gọi mạng.
     */
    public void kiemTraCauHinh() throws AiException {
        docCauHinh();
    }

    public String taoVanBan(
            String huongDan,
            String yeuCau
    ) throws AiException {
        return goiApi(huongDan, yeuCau, false);
    }

    public JsonObject taoJson(
            String huongDan,
            String yeuCau
    ) throws AiException {
        String ketQua = goiApi(huongDan, yeuCau, true);
        return docDoiTuongJson(ketQua);
    }

    private String goiApi(
            String huongDan,
            String yeuCau,
            boolean traJson
    ) throws AiException {

        kiemTraDauVao(huongDan, yeuCau);

        CauHinh cauHinh = docCauHinh();

        JsonObject noiDungGui = new JsonObject();

        noiDungGui.add(
                "systemInstruction",
                taoContent(null, huongDan.trim())
        );

        JsonArray contents = new JsonArray();
        contents.add(
                taoContent("user", yeuCau.trim())
        );
        noiDungGui.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("candidateCount", 1);
        generationConfig.addProperty("maxOutputTokens", 4096);
        generationConfig.addProperty(
                "responseMimeType",
                traJson ? "application/json" : "text/plain"
        );

        noiDungGui.add(
                "generationConfig",
                generationConfig
        );

        URI diaChi = URI.create(
                DIA_CHI_API
                        + cauHinh.model()
                        + ":generateContent"
        );

        HttpRequest request = HttpRequest.newBuilder(diaChi)
                .timeout(Duration.ofSeconds(30))
                .header(
                        "Content-Type",
                        "application/json; charset=utf-8"
                )
                .header("Accept", "application/json")
                .header("x-goog-api-key", cauHinh.khoa())
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                JSON.toJson(noiDungGui),
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response;

        try {
            response = HTTP.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (HttpTimeoutException e) {
            throw new AiException(
                    "API AI phản hồi quá thời gian cho phép."
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new AiException(
                    "Tác vụ gọi AI đã bị ngắt."
            );
        } catch (IOException e) {
            throw new AiException(
                    "Không kết nối được API AI. "
                            + "Kiểm tra Internet và kết nối HTTPS."
            );
        }

        if (response.statusCode() != 200) {
            throw new AiException(
                    taoLoiHttp(
                            response.statusCode(),
                            response.body(),
                            cauHinh.khoa()
                    )
            );
        }

        String duLieu = response.body();

        if (duLieu == null
                || duLieu.isBlank()
                || duLieu.length() > 262_144) {
            throw new AiException(
                    "Phản hồi API AI rỗng hoặc vượt giới hạn."
            );
        }

        return layVanBan(
                docDoiTuongJson(duLieu)
        );
    }

    private static String layVanBan(
            JsonObject phanHoi
    ) throws AiException {

        JsonObject feedback = doiTuong(
                phanHoi.get("promptFeedback")
        );

        String lyDoChan = docChuoi(
                feedback,
                "blockReason"
        );

        if (!lyDoChan.isBlank()
                && !"BLOCK_REASON_UNSPECIFIED".equals(lyDoChan)) {
            throw new AiException(
                    "AI không tạo nội dung cho yêu cầu này."
            );
        }

        JsonElement candidatesValue = phanHoi.get("candidates");

        if (candidatesValue == null
                || !candidatesValue.isJsonArray()) {
            throw new AiException(
                    "API AI không trả về danh sách kết quả."
            );
        }

        JsonArray candidates = candidatesValue.getAsJsonArray();

        if (candidates.size() != 1) {
            throw new AiException(
                    "API AI trả về số lượng kết quả không hợp lệ."
            );
        }

        JsonObject candidate = doiTuong(candidates.get(0));

        if (candidate == null) {
            throw new AiException(
                    "Kết quả AI sai cấu trúc."
            );
        }

        String lyDoKetThuc = docChuoi(
                candidate,
                "finishReason"
        );

        if ("MAX_TOKENS".equals(lyDoKetThuc)) {
            throw new AiException(
                    "Kết quả AI bị cắt do đạt giới hạn token."
            );
        }

        if (!"STOP".equals(lyDoKetThuc)) {
            throw new AiException(
                    "AI chưa trả về kết quả hoàn chỉnh."
            );
        }

        JsonObject content = doiTuong(
                candidate.get("content")
        );

        JsonElement partsValue = content == null
                ? null
                : content.get("parts");

        if (partsValue == null || !partsValue.isJsonArray()) {
            throw new AiException(
                    "Kết quả AI thiếu nội dung văn bản."
            );
        }

        StringBuilder ketQua = new StringBuilder();

        for (JsonElement phan : partsValue.getAsJsonArray()) {
            JsonObject part = doiTuong(phan);

            if (part == null) {
                throw new AiException(
                        "Một phần nội dung AI sai cấu trúc."
                );
            }

            JsonElement thought = part.get("thought");

            if (thought != null && !thought.isJsonNull()) {
                if (!thought.isJsonPrimitive()
                        || !thought.getAsJsonPrimitive()
                        .isBoolean()) {
                    throw new AiException(
                            "Phản hồi AI có trường thought không hợp lệ."
                    );
                }

                if (thought.getAsBoolean()) {
                    continue;
                }
            }

            JsonElement text = part.get("text");

            if (text == null || text.isJsonNull()) {
                continue;
            }

            if (!text.isJsonPrimitive()
                    || !text.getAsJsonPrimitive().isString()) {
                throw new AiException(
                        "Phản hồi AI có trường text không hợp lệ."
                );
            }

            // Nối nguyên các phần để không làm hỏng JSON bị chia nhỏ.
            ketQua.append(text.getAsString());
        }

        String vanBan = ketQua.toString().trim();

        if (vanBan.isEmpty() || vanBan.length() > 20_000) {
            throw new AiException(
                    "Nội dung AI rỗng hoặc vượt giới hạn."
            );
        }

        return vanBan;
    }

    private static CauHinh docCauHinh() throws AiException {
        String khoa = docBienMoiTruong("AI_API_KEY");
        String model = docBienMoiTruong("AI_MODEL");

        if (khoa.isEmpty()) {
            throw new AiException(
                    "Thiếu biến môi trường AI_API_KEY."
            );
        }

        if (!khoa.matches("[\\x21-\\x7E]+")) {
            throw new AiException(
                    "AI_API_KEY chứa khoảng trắng hoặc ký tự không hợp lệ."
            );
        }

        if (model.startsWith("models/")) {
            model = model.substring("models/".length());
        }

        if (model.isEmpty()) {
            throw new AiException(
                    "Thiếu biến môi trường AI_MODEL."
            );
        }

        if (!model.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new AiException(
                    "AI_MODEL không đúng định dạng mã model."
            );
        }

        return new CauHinh(khoa, model);
    }

    private static void kiemTraDauVao(
            String huongDan,
            String yeuCau
    ) throws AiException {

        if (huongDan == null || huongDan.isBlank()) {
            throw new AiException(
                    "Thiếu hướng dẫn cho AI."
            );
        }

        if (yeuCau == null || yeuCau.isBlank()) {
            throw new AiException(
                    "Thiếu nội dung yêu cầu AI."
            );
        }

        if (huongDan.length() > 6_000
                || yeuCau.length() > 40_000) {
            throw new AiException(
                    "Nội dung yêu cầu AI vượt giới hạn."
            );
        }
    }

    private static JsonObject taoContent(
            String role,
            String vanBan
    ) {
        JsonObject text = new JsonObject();
        text.addProperty("text", vanBan);

        JsonArray parts = new JsonArray();
        parts.add(text);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        if (role != null) {
            content.addProperty("role", role);
        }

        return content;
    }

    private static JsonObject docDoiTuongJson(
            String duLieu
    ) throws AiException {
        try {
            JsonElement ketQua = JSON.fromJson(
                    duLieu,
                    JsonElement.class
            );

            if (ketQua == null || !ketQua.isJsonObject()) {
                throw new AiException(
                        "Kết quả phải là một đối tượng JSON."
                );
            }

            return ketQua.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new AiException(
                    "API AI trả về JSON không hợp lệ."
            );
        }
    }

    private static JsonObject doiTuong(JsonElement giaTri) {
        return giaTri != null && giaTri.isJsonObject()
                ? giaTri.getAsJsonObject()
                : null;
    }

    private static String docChuoi(
            JsonObject doiTuong,
            String tenTruong
    ) {
        if (doiTuong == null) {
            return "";
        }

        JsonElement giaTri = doiTuong.get(tenTruong);

        if (giaTri == null
                || !giaTri.isJsonPrimitive()
                || !giaTri.getAsJsonPrimitive().isString()) {
            return "";
        }

        return giaTri.getAsString();
    }

    private static String taoLoiHttp(
            int maHttp,
            String duLieu,
            String khoa
    ) {
        String thongDiep = switch (maHttp) {
            case 400 -> "API AI từ chối yêu cầu (HTTP 400).";
            case 401, 403 ->
                    "Không xác thực được hoặc chưa có quyền gọi AI "
                            + "(HTTP " + maHttp + ").";
            case 404 ->
                    "Không tìm thấy model hoặc API "
                            + "(HTTP 404). Kiểm tra AI_MODEL.";
            case 429 ->
                    "API AI đang giới hạn tần suất hoặc hạn mức "
                            + "(HTTP 429).";
            default ->
                    "Gọi API AI thất bại (HTTP " + maHttp + ").";
        };

        if (duLieu != null
                && !duLieu.isBlank()
                && duLieu.length() <= 32_768) {
            try {
                JsonObject root = docDoiTuongJson(duLieu);
                JsonObject error = doiTuong(root.get("error"));

                String chiTiet = docChuoi(error, "message");

                if (!chiTiet.isBlank()) {
                    thongDiep += " " + chiTiet;
                }
            } catch (AiException ignored) {
                // Giữ thông báo theo mã HTTP nếu body không phải JSON.
            }
        }

        thongDiep = thongDiep.replace(khoa, "***");

        return thongDiep.length() > 700
                ? thongDiep.substring(0, 700)
                : thongDiep;
    }

    private static String docBienMoiTruong(String tenBien) {
        String giaTri = System.getenv(tenBien);
        return giaTri == null ? "" : giaTri.trim();
    }
}