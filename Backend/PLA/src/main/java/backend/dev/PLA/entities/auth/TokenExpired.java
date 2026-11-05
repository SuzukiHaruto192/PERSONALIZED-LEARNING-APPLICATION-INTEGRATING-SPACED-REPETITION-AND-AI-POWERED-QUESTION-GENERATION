package backend.dev.PLA.entities.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "token_expired")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenExpired {
    @Id
    @Column(name = "id", length = 255)
    String id;

    @Column(name = "expired_at", nullable = false)
    Instant expiredAt;
}
