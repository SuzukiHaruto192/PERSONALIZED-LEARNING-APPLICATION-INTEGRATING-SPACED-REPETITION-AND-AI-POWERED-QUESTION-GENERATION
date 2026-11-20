package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một cặp (skill, topic) đến hạn ôn trong GET /api/v1/progress/due. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DueTopicProgressResponse {
    RefResponse skill;
    ProgressTopicResponse topic;
    // Số ngày đã quá hạn tính tới hôm nay (0 = đến hạn đúng hôm nay)
    long overdueDays;
}
