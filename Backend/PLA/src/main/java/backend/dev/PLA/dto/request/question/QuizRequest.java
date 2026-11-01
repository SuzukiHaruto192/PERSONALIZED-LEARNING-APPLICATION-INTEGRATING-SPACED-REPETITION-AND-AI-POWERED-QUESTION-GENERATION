package backend.dev.PLA.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizRequest {
    @NotBlank(message = "TOPIC_BLANK")
    Long id_topic;

    @NotBlank(message = "SKILL_BLANK")
    Long id_skill;

    @Size(min = 1 , max =50 , message = "QUIZ_NAME_SIZE")
    String name;

}
