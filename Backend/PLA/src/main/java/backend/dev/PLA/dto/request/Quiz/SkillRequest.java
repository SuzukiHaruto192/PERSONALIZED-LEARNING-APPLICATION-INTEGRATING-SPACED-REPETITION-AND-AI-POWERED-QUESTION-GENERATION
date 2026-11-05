package backend.dev.PLA.dto.request.Quiz;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillRequest {

    @NotBlank(message = "name không được để trống")
    @Size(max = 50, message = "name tối đa 50 kí tự")
    String name;

    @NotBlank(message = "code không được để trống")
    @Size(max = 20, message = "code tối đa 20 kí tự")
    @Pattern(regexp = "^[A-Z]+(?:_[A-Z]+)*$", message = "code chỉ gồm chữ in hoa và dấu gạch dưới (vd: GRAMMAR, VOCAB)")
    String code;

    @Size(max = 50, message = "icon tối đa 50 kí tự")
    String icon;
}
