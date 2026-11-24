package backend.dev.PLA.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quyền phải lấy từ role trong DB, không tin claim "scope" của token. */
@ExtendWith(MockitoExtension.class)
class CustomJwtAuthenticationConverterTest {

    @Mock
    CustomUserDetailService customUserDetailService;

    @InjectMocks
    CustomJwtAuthenticationConverter converter;

    @Test
    void tokenScopeUser_butDbAdmin_getsRoleAdmin() {
        mockUser("alice", Role.ADMIN);

        AbstractAuthenticationToken auth = converter.convert(jwt("alice", "USER"));

        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(auth.getPrincipal()).isInstanceOf(CustomUserDetail.class);
    }

    @Test
    void tokenScopeAdmin_butDbDemotedToUser_getsRoleUserOnly() {
        mockUser("bob", Role.USER);

        AbstractAuthenticationToken auth = converter.convert(jwt("bob", "ADMIN"));

        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    private void mockUser(String username, Role role) {
        when(customUserDetailService.loadUserByUsername(username))
                .thenReturn(new CustomUserDetail(User.builder()
                        .id(1L)
                        .username(username)
                        .password("p")
                        .role(role)
                        .build()));
    }

    private static Jwt jwt(String subject, String scope) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS512")
                .subject(subject)
                .claim("scope", scope)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }
}
