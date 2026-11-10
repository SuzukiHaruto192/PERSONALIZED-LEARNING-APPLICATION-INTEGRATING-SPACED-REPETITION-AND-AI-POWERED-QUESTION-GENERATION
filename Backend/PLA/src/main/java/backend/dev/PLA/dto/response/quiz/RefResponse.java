package backend.dev.PLA.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Tham chiếu gọn { id, name } tới skill / topic / format trong response quiz
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefResponse {
    Long id;
    String name;
}
