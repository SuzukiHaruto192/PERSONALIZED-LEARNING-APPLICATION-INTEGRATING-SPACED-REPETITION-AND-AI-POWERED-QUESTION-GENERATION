package backend.dev.PLA.dto.request.Quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Một câu trả lời trong body PUT /api/v1/quizzes/{id}/attempts. SINGLE_CHOICE gửi optionId, FILL_BLANK gửi
 * answerText. Bỏ trống cả hai = bỏ qua câu (được lưu, tính sai).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptAnswerRequest {
    @NotNull(message = "ATTEMPT_QUESTION_ID_BLANK")
    @Schema(example = "15")
    Long questionId;

    @Schema(example = "31", nullable = true, description = "Chỉ dùng cho câu SINGLE_CHOICE")
    Long optionId;

    @Size(max = 255, message = "ATTEMPT_ANSWER_TEXT_SIZE")
    @Schema(example = "goes", nullable = true, description = "Chỉ dùng cho câu FILL_BLANK")
    String answerText;
}
