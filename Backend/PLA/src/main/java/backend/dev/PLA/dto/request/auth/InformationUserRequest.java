package backend.dev.PLA.dto.request.auth;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InformationUserRequest {

    String imageUrl;

    @DecimalMin(value = "0.0", message = "CURRENT_SCORE_MIN")
    @DecimalMax(value = "9.0", message = "CURRENT_SCORE_MAX")
    BigDecimal currentScore;

    @DecimalMin(value = "0.0", message = "TARGET_SCORE_MIN")
    @DecimalMax(value = "9.0", message = "TARGET_SCORE_MAX")
    BigDecimal targetScore;

    @Size(min = 6, max = 50, message = "PASSWORD_INVALID_SIZE")
    @Pattern(
            regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "PASSWORD_INVALID_PATTERN"
    )
    String password;

    @Email(message = "EMAIL_INVALID_FORMAT")
    String email;

    @Size(min = 10, max = 15, message = "PHONE_INVALID_SIZE")
    @Pattern(regexp = "^[0-9]+$", message = "PHONE_INVALID_PATTERN")
    String phoneNumber;
}