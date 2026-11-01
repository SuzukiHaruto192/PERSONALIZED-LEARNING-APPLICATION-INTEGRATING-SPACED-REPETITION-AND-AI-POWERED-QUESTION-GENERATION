package backend.dev.PLA.dto.request.flashcard;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignupRequest {

    @NotBlank(message = "USERNAME_BLANK")
    @Size(min = 6, max = 50, message = "USERNAME_INVALID_SIZE")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "USERNAME_INVALID_PATTERN")
    String username;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 6, max = 50, message = "PASSWORD_INVALID_SIZE")
    @Pattern(
            regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "PASSWORD_INVALID_PATTERN"
    )
    String password;

    @NotBlank(message = "EMAIL_BLANK")
    @Email(message = "EMAIL_INVALID_FORMAT")
    String email;

    @NotBlank(message = "PHONE_BLANK")
    @Size(min = 10, max = 15, message = "PHONE_INVALID_SIZE")
    @Pattern(regexp = "^[0-9]+$", message = "PHONE_INVALID_PATTERN")
    String phoneNumber;
}