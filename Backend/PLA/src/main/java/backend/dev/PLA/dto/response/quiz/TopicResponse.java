package backend.dev.PLA.dto.response.quiz;

import backend.dev.PLA.enums.CefrLevel;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicResponse {
    Long id;
    Long groupId;
    String groupName;
    String slug;
    String nameVi;
    String nameEn;
    String keyword;
    String icon;
    CefrLevel level;
    String description;
    Integer sortBy;
    Boolean isActive;
    List<SkillResponse> skills;
}
