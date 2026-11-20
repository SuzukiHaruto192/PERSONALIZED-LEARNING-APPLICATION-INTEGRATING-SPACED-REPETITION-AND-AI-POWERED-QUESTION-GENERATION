package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.CefrLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Topic rút gọn lồng trong DueTopicProgressResponse (không kèm group / skills để tránh lặp dữ liệu). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressTopicResponse {
    Long id;
    String slug;
    String nameVi;
    String nameEn;
    CefrLevel cefrLevel;
}
