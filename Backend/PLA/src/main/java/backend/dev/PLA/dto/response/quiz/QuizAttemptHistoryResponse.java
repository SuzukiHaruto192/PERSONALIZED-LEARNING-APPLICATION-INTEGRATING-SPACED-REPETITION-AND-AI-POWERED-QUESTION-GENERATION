package backend.dev.PLA.dto.response.quiz;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response của GET /api/v1/quizzes/{id}/attempts: quiz chi tiết (passages, questions kèm options) và các lượt làm
 * của user hiện tại, mới nhất trước. Ghép câu trả lời với câu hỏi qua answers[].questionId.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAttemptHistoryResponse {
    QuizResponse quiz;
    List<AttemptResponse> attempts;
}
