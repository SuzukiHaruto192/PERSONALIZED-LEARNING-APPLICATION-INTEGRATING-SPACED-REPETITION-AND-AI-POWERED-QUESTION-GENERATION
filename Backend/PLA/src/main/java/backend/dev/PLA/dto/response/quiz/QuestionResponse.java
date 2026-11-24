package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.QuestionType;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionResponse {
    Long id;
    Long quizId;
    Long passageId;
    Short position;
    QuestionType type;
    String content;
    String correctText;
    String explanation;
    RefResponse grammarTopic;

    // Chỉ đọc, sắp theo position. Đáp án do AI sinh cùng quiz; chỉ xoá được qua DELETE /api/v1/options/{id}
    List<OptionResponse> options;

    // SINGLE_CHOICE: 2-6 đáp án và đúng 1 đáp án đúng; FILL_BLANK: có correctText. Boolean để JSON là "isComplete"
    Boolean isComplete;
}
