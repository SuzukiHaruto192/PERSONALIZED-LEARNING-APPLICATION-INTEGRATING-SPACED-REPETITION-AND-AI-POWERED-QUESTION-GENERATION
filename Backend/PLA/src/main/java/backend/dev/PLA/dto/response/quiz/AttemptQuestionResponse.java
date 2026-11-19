package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Cặp <câu hỏi (kèm options), câu trả lời của user> trong một lượt làm. answer = null: câu chưa trả lời. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptQuestionResponse {
    QuestionResponse question;
    AttemptAnswerResponse answer;
}
