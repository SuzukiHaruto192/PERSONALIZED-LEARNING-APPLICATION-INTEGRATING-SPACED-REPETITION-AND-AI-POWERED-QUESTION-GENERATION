package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.TopicGroupType;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicGroupResponse {
    Long id;
    TopicGroupType type;
    String name;
    String icon;
    Integer orderBy;
    List<TopicResponse> topics;
}
