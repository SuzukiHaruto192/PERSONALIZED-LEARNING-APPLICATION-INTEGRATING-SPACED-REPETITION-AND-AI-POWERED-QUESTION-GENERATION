package backend.dev.PLA.dto.request.Quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE )
public class TopicRequest {
    String slug;
    String nameVi;
    String nameEn;

    String keyword;
    String icon;
    String level;
    String description;
    Integer sortBy;
    Boolean isActive;
}
