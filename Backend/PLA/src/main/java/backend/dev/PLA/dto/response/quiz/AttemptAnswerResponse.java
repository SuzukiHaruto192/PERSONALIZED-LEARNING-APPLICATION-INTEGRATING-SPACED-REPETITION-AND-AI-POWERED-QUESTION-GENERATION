package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptAnswerResponse {
    Long attemptId;
    Long questionId;
    Long selectedOptionId;
    String answerText;

    // Boolean để JSON đúng tên "isCorrect"
    Boolean isCorrect;
}
