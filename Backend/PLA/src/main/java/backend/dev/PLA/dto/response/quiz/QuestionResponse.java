package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    // Đáp án: chỉ có trong response nộp bài và lịch sử lượt làm; API lấy đề để null → không có trong JSON
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String correctText;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String explanation;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    RefResponse grammarTopic;

    // Chỉ đọc, sắp theo position. Đáp án do AI sinh cùng quiz; chỉ xoá được qua DELETE /api/v1/options/{id}
    List<OptionResponse> options;

    // SINGLE_CHOICE: 2-6 đáp án và đúng 1 đáp án đúng; FILL_BLANK: có correctText. Boolean để JSON là "isComplete".
    // Cho biết số đáp án đúng nên cũng ẩn khi lấy đề
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isComplete;
}
