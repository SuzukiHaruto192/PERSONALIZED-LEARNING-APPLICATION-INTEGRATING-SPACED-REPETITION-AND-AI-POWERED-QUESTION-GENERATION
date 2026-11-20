package backend.dev.PLA.dto.request.flashcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body của PUT /api/v1/flashcards/{flashCardId}: chỉ sửa nội dung, không đổi lịch ôn. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFlashCardRequest {
    @NotBlank(message = "FRONT_CONTENT_BLANK")
    @Size(max = 255, message = "FRONT_CONTENT_SIZE")
    String frontContent;

    @NotBlank(message = "BACK_CONTENT_BLANK")
    @Size(max = 255, message = "BACK_CONTENT_SIZE")
    String backContent;

    @Size(max = 1000, message = "FLASHCARD_EXAMPLE_SIZE")
    String example;
}
