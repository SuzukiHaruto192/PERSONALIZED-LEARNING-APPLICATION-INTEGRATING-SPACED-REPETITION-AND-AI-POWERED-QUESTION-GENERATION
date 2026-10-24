package backend.dev.PLA.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;

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
    @Column(name = "id" , length = 255)
    String id;

    @Column(name = "expiration_time")
    Instant expiredAt;
}
