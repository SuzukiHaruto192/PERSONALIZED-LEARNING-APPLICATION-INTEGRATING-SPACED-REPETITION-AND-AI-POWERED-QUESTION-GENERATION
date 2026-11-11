package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.QuizStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResponse {
    Long id;
    String title;
    String description;
    RefResponse skill;
    RefResponse topic;
    String customTopic;
    RefResponse format;
    Short difficulty;
    QuizStatus status;
    Long createdBy;
    Instant createdAt;
    long questionCount;

    // Chỉ có ở response chi tiết (GET /api/v1/quizzes/{id}), sắp theo position; danh sách quiz không trả field này
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<PassageResponse> passages;
}
