package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PassageResponse {
    Long id;
    Long quizId;
    Short position;
    String content;
    String audioUrl;
    long questionCount;
}
