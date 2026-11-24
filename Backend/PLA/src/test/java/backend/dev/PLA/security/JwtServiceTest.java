package backend.dev.PLA.security;

import static org.assertj.core.api.Assertions.assertThat;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.Role;
import backend.dev.PLA.repositories.auth.TokenExpiredRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        byte[] secret = new byte[64];
        new SecureRandom().nextBytes(secret);
        SecretKeySpec key = new SecretKeySpec(secret, SecurityConfiguration.JWT_ALGORITHM.getName());
        jwtService = new JwtService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)), Mockito.mock(TokenExpiredRepository.class));
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
    }

    @Test
    void adminPrincipal_scopeIsAdmin() throws Exception {
        String token = jwtService.createAccessToken(authOf(Role.ADMIN));

        assertThat(scopeOf(token)).isEqualTo("ADMIN");
        assertThat(SignedJWT.parse(token).getJWTClaimsSet().getSubject()).isEqualTo("alice");
    }

    @Test
    void userPrincipal_scopeIsUser() throws Exception {
        assertThat(scopeOf(jwtService.createAccessToken(authOf(Role.USER)))).isEqualTo("USER");
    }

    @Test
    void principalNotCustomUserDetail_scopeFallsBackToUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(scopeOf(jwtService.createAccessToken(auth))).isEqualTo("USER");
    }

    private static Authentication authOf(Role role) {
        CustomUserDetail detail = new CustomUserDetail(
                User.builder().id(1L).username("alice").password("p").role(role).build());
        return new UsernamePasswordAuthenticationToken(detail, null, detail.getAuthorities());
    }

    private static String scopeOf(String token) throws Exception {
        return SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("scope");
    }
}
