package backend.dev.PLA.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "The username cannot be left blank")
    @Size(min= 6 , max =50 , message = "The username must between 6 and 50 characters long.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$" , message = "The username must consist of letter and number.")
    private String username;

    @NotBlank(message = "The password cannot be left blank.")
    @Size(min = 6 , max = 50 , message = "The password must between 6 and 50 characters long.")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
    message = "The password must consist of least one uppercase letters , lowercase letter , number and special character  ")
    private String password;
}
