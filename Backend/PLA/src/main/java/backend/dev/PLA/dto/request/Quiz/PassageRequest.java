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

/** Body của POST /api/v1/quizzes/{quizId}/passages và PUT /api/v1/passages/{id}. */
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
            description = "Bỏ trống: POST xếp cuối (max + 1), PUT giữ nguyên vị trí cũ. Trùng passage khác → 400")
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
