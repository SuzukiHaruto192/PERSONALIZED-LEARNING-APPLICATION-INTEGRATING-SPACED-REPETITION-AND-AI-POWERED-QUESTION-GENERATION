package backend.dev.PLA.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = "The username cannot be left blank")
    @Size(min= 6 , max =50 , message = "The username must between 6 and 50 characters long.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$" , message = "The username must consist of letter and number.")
    private String username;

    @NotBlank(message = "The password cannot be left blank.")
    @Size(min = 6 , max = 50 , message = "The password must between 6 and 50 characters long.")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$",
            message = "The password must consist of least one uppercase letters , lowercase letter , number and special character  ")
    private String password;

    @NotBlank(message = "email cannot be left blank.")
    @Email(message = "you must to enter email")
    private String email ;

    @NotBlank(message = "phone number cannot be left blank.")
    @Size(min=10 , max=15 , message = "The phoneNumber must between 10 and 15 number long.")
    @Pattern(regexp = "^[0-9]+$" , message = "The phone Number consist of only number.")
    private String phoneNumber;
}
