package backend.dev.PLA.dto.request.Quiz;

import backend.dev.PLA.enums.CefrLevel;
import jakarta.validation.constraints.*;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicRequest {

    @NotNull(message = "id TopicGroup không được để trống ")
    Long idTopicGroup;

    @NotBlank(message = "slug không được để trống")
    @Size(max = 100, message = "slug tối đa 100 kí tự")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "slug chỉ gồm chữ thường, số và dấu gạch ngang (vd: daily-life)")
    String slug;

    @NotBlank(message = "nameVi không được để trống")
    @Size(max = 100, message = "nameVi tối đa 100 kí tự")
    String nameVi;

    @Size(max = 100, message = "nameEn tối đa 100 kí tự")
    String nameEn;

    @Size(max = 255, message = "keyword tối đa 255 kí tự")
    String keyword;

    @Size(max = 50, message = "icon tối đa 50 kí tự")
    String icon;

    @NotNull(message = "level phải là : A1,A2 , ... , C2 ")
    CefrLevel level;

    @Size(max = 2000, message = "description tối đa 2000 kí tự")
    String description;

    @PositiveOrZero(message = "sortBy bằng 0 hoặc dương")
    Integer sortBy;

    @NotEmpty(message = "topic phải thuộc ít nhất 1 skill")
    Set<@NotNull(message = "id skill không được null") Long> skillIds;
}
