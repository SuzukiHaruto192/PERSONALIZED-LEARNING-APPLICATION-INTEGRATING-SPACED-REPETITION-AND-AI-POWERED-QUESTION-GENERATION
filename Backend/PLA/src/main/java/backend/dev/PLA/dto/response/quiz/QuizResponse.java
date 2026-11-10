package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.QuizStatus;
import java.time.Instant;
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
}
