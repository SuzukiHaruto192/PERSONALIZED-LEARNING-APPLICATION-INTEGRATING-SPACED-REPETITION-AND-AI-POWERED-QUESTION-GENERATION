package backend.dev.PLA.dto.response.user;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private BigDecimal currentScore;
    private BigDecimal targetScore;
    private String avatarUrl;
}
