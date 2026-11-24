package backend.dev.PLA.dto.request.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu tạo passage cho PassageService.createPassage. Không còn endpoint POST/PUT: passage do AI sinh, được
 * GeneratedQuizSaveService chuyển sang DTO này rồi lưu. Annotation validation giữ lại để mô tả ràng buộc của passage.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassageRequest {
    static final int AUDIO_URL_MAX_LENGTH = 500;

    @Min(value = 1, message = "PASSAGE_POSITION_INVALID")
    @Max(value = Short.MAX_VALUE, message = "PASSAGE_POSITION_INVALID")
    @Schema(
            nullable = true,
            minimum = "1",
            description = "Bỏ trống: xếp cuối (max + 1). Trùng passage khác → PASSAGE_POSITION_DUPLICATED")
    Integer position;

    @Schema(example = "Tom gets up at 6 a.m. every day. He ...", nullable = true)
    String content;

    @Schema(
            example = "https://cdn.example.com/audio/routine-01.mp3",
            nullable = true,
            maxLength = AUDIO_URL_MAX_LENGTH,
            description = "URL http/https")
    String audioUrl;

    // Tương ứng chk_passage_content: phải có ít nhất một trong hai (chuỗi rỗng coi như không có)
    @JsonIgnore
    @AssertTrue(message = "PASSAGE_CONTENT_REQUIRED")
    public boolean isContentOrAudioPresent() {
        return isPresent(content) || isPresent(audioUrl);
    }

    @JsonIgnore
    @AssertTrue(message = "PASSAGE_AUDIO_URL_INVALID")
    public boolean isAudioUrlValid() {
        if (!isPresent(audioUrl)) return true;
        String url = audioUrl.trim();
        if (url.length() > AUDIO_URL_MAX_LENGTH) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
