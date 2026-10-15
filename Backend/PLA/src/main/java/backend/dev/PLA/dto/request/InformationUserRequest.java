package backend.dev.PLA.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationUserRequest {

    private String imageUrl;

    @DecimalMin(value = "0.0", message = "CURRENT_SCORE_MIN")
    @DecimalMax(value = "9.0", message = "CURRENT_SCORE_MAX")
    private BigDecimal currentScore;

    @DecimalMin(value = "0.0", message = "TARGET_SCORE_MIN")
    @DecimalMax(value = "9.0", message = "TARGET_SCORE_MAX")
    private BigDecimal targetScore;

    @Size(min = 6, max = 50, message = "PASSWORD_INVALID_SIZE")
    @Pattern(
            regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "PASSWORD_INVALID_PATTERN"
    )
    private String password;

    @Email(message = "EMAIL_INVALID_FORMAT")
    private String email;

    @Size(min = 10, max = 15, message = "PHONE_INVALID_SIZE")
    @Pattern(regexp = "^[0-9]+$", message = "PHONE_INVALID_PATTERN")
    private String phoneNumber;
}