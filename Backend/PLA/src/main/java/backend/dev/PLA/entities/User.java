package backend.dev.PLA.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", length = 15, nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "current_score" , precision = 4 ,  scale = 2)
    private BigDecimal currentScore;

    @Column(name = "target_score" , precision = 2 , scale = 2)
    private BigDecimal targetScore;

    @Column(name = "avatar_url" ,  length = 255)
    private String avatarUrl;
}