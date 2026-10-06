package backend.dev.PLA.dto;

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
    @DecimalMin(value = "0.0" , message = "The minimum current score must be 0.0")
    @DecimalMax(value = "9.0" , message = "The maximum current score must be 9.0")
    private BigDecimal currentScore;
    @DecimalMin(value = "0.0" , message = "The minimum target score must be 0.0")
    @DecimalMax(value = "9.0" , message = "The maximum target score must be 9.0")
    private BigDecimal targetScore;

    @Size(min = 6 , max = 50 , message = "The password must between 6 and 50 characters long.")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "The password must consist of least one uppercase letters , lowercase letter , number and special character  ")
    private String password;

    @Email(message = "you must to enter email")
    private String email;

    @Size(min=10 , max=15 , message = "The phoneNumber must between 10 and 15 number long.")
    @Pattern(regexp = "^[0-9]+$" , message = "The phone Number consist of only number.")
    private String phoneNumber;
}
