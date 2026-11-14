package backend.dev.PLA.dto.request.auth;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InformationUserRequest {

    String imageUrl;

    // Điểm mục tiêu người dùng muốn đạt (thang 0 - 10)
    @DecimalMin(value = "0.0", message = "TARGET_SCORE_MIN")
    @DecimalMax(value = "10.0", message = "TARGET_SCORE_MAX")
    BigDecimal currentScore;

    @Size(min = 6, max = 50, message = "PASSWORD_INVALID_SIZE")
    @Pattern(
            regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "PASSWORD_INVALID_PATTERN")
    String password;

    // Bắt buộc khi đổi mật khẩu (password khác rỗng)
    String oldPassword;

    @Email(message = "EMAIL_INVALID_FORMAT")
    String email;

    @Size(min = 10, max = 15, message = "PHONE_INVALID_SIZE")
    @Pattern(regexp = "^[0-9]+$", message = "PHONE_INVALID_PATTERN")
    String phoneNumber;
}
