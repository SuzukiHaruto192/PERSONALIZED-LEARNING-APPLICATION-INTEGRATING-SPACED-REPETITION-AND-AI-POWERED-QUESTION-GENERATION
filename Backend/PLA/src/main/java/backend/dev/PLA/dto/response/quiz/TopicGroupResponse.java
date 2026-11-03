package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.TopicGroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE )
public class TopicGroupResponse {
    Long id;
    TopicGroupType type;
    String name;
    String icon;
    Integer orderBy;
}
