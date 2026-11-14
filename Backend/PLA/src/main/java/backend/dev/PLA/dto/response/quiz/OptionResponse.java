package backend.dev.PLA.dto.response.quiz;

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

    // Boolean để JSON đúng tên "isCorrect" (boolean nguyên thuỷ sẽ thành "correct")
    Boolean isCorrect;
}
