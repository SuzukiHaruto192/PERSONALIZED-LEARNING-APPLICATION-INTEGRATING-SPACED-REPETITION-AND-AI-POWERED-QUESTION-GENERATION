package backend.dev.PLA.dto.response.quiz;

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
public class AttemptResponse {
    Long id;

    RefResponse quiz;

    RefResponse skill;
    Instant startedAt;
    Instant submittedAt;
    Integer correctCount;
    Integer totalQuestions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<AttemptQuestionResponse> questions;
}
