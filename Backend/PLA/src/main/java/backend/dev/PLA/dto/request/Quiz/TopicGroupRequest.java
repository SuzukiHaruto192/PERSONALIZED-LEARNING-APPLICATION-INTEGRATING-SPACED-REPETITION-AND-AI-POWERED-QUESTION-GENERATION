package backend.dev.PLA.dto.request.Quiz;

import backend.dev.PLA.enums.TopicGroupType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicGroupRequest {

    @NotNull(message = "type không được để trống")
    TopicGroupType type;

    @NotBlank(message = "name không được để trống")
    @Size(max = 100)
    String name;

    @Size(max = 50)
    String icon;

    @NotNull(message = "sortOrder không được null")
    @PositiveOrZero(message = "sortOrder bằng 0 hoặc dương")
    Integer sortOrder;
}