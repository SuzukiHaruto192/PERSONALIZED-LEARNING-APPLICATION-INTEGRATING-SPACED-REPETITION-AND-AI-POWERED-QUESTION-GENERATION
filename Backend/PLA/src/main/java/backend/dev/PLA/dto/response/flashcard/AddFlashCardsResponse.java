package backend.dev.PLA.dto.response.flashcard;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddFlashCardsResponse {
    // Số thẻ được thêm mới vào deck
    int added;
    // Số thẻ bỏ qua vì đã có trong deck
    int skipped;
}
