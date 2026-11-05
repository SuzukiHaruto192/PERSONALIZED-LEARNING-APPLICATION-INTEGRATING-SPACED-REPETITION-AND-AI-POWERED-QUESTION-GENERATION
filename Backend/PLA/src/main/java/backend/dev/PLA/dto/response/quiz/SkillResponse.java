package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
