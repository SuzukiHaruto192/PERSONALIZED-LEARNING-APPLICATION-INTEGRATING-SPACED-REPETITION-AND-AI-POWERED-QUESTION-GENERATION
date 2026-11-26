package backend.dev.PLA.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptionResponse {
    Long id;
    Long questionId;
    Short position;
    String content;

    // Boolean để JSON đúng tên "isCorrect" (boolean nguyên thuỷ sẽ thành "correct").
    // Chỉ có trong response nộp bài và lịch sử lượt làm; API lấy đề để null → không có trong JSON
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isCorrect;
}
