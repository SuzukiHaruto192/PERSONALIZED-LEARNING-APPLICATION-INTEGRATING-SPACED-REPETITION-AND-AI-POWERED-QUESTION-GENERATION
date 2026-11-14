package backend.dev.PLA.dto.response.quiz;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillResponse {
    Long id;
    String name;
    String code;
    String icon;
    List<TopicGroupResponse> topicGroups;
}
