package backend.dev.PLA.dto.request.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body của PUT /api/v1/quizzes/{quizId}/questions/order. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionOrderRequest {
    @NotNull(message = "QUESTION_ORDER_INVALID")
    @Schema(
            example = "[15, 12, 13, 14]",
            description = "Đúng và đủ id mọi câu hỏi của quiz, không trùng. Câu đầu danh sách có position = 1")
    List<Long> questionIds;
}
