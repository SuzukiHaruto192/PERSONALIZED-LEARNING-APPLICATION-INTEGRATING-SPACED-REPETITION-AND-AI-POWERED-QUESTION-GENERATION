package backend.dev.PLA.dto.request.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Body của PUT /api/v1/quizzes/{id}/attempts. quizId lấy từ path, user lấy từ token: nếu client gửi kèm quizId /
 * userId trong body thì bị bỏ qua (không tin user id do client gửi).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttemptRequest {
    @Valid
    @NotEmpty(message = "ATTEMPT_ANSWERS_EMPTY")
    List<AttemptAnswerRequest> answers;
}
