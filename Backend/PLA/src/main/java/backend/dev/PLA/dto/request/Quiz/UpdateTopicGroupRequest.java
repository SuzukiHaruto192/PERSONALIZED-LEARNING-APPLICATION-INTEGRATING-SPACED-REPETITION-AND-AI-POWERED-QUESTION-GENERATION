package backend.dev.PLA.dto.request.Quiz;

import backend.dev.PLA.enums.TopicGroupType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request cập nhật một phần topic group.
 * Trường nào null thì giữ nguyên giá trị cũ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTopicGroupRequest {

    TopicGroupType type;

    @Pattern(regexp = ".*\\S.*", message = "name không được để trống")
    @Size(max = 100)
    String name;

    @Size(max = 50)
    String icon;

    @PositiveOrZero(message = "sortOrder bằng 0 hoặc dương")
    Integer sortOrder;
}
