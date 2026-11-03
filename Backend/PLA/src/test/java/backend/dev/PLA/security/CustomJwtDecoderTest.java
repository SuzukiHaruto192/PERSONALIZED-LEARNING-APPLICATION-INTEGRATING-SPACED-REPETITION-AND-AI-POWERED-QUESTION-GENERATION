package backend.dev.PLA.security;

import backend.dev.PLA.repositories.TokenExpiredRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomJwtDecoderTest {

    @Mock
    TokenExpiredRepository tokenExpiredRepository;

    byte[] secret;
    CustomJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        secret = randomKey();
        decoder = new CustomJwtDecoder(Base64.getEncoder().encodeToString(secret), tokenExpiredRepository);
    }

    // ===== Token hợp lệ =====

    @Test
    void decode_validToken_returnsJwtAndChecksBlacklistOnce() throws Exception {
        String jti = UUID.randomUUID().toString();
        String token = sign(secret, JWSAlgorithm.HS512, claims(jti, Instant.now().plus(1, ChronoUnit.HOURS)));
        when(tokenExpiredRepository.existsById(jti)).thenReturn(false);

        Jwt jwt = decoder.decode(token);

        assertEquals("alice", jwt.getSubject());
        assertEquals(jti, jwt.getId());
        assertEquals("USER", jwt.getClaimAsString("scope"));
        verify(tokenExpiredRepository).existsById(jti);
    }

    // ===== Blacklist / jti =====

    @Test
    void decode_loggedOutToken_throwsBadJwtException() throws Exception {
        String jti = UUID.randomUUID().toString();
        String token = sign(secret, JWSAlgorithm.HS512, claims(jti, Instant.now().plus(1, ChronoUnit.HOURS)));
        when(tokenExpiredRepository.existsById(jti)).thenReturn(true);

        assertThrows(BadJwtException.class, () -> decoder.decode(token));
    }

    @Test
    void decode_tokenWithoutJti_throwsBadJwtExceptionWithoutQueryingDb() throws Exception {
        String token = sign(secret, JWSAlgorithm.HS512, claims(null, Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThrows(BadJwtException.class, () -> decoder.decode(token));
        verify(tokenExpiredRepository, never()).existsById(anyString());
    }

    // ===== Chữ ký / thuật toán / định dạng: bị Nimbus chặn trước khi tra DB =====

    @Test
    void decode_expiredToken_throwsWithoutClockSkew() throws Exception {
        // Hết hạn 5 giây trước: Nimbus mặc định cho lệch 60s sẽ vẫn chấp nhận,
        // decoder đặt clock skew = 0 nên phải từ chối.
        String token = sign(secret, JWSAlgorithm.HS512,
                claims(UUID.randomUUID().toString(), Instant.now().minusSeconds(5)));

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
        verify(tokenExpiredRepository, never()).existsById(anyString());
    }

    @Test
    void decode_tokenSignedWithOtherKey_throwsBadJwtException() throws Exception {
        String token = sign(randomKey(), JWSAlgorithm.HS512,
                claims(UUID.randomUUID().toString(), Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThrows(BadJwtException.class, () -> decoder.decode(token));
        verify(tokenExpiredRepository, never()).existsById(anyString());
    }

    @Test
    void decode_tokenWithOtherAlgorithm_throwsBadJwtException() throws Exception {
        // Cùng secret nhưng ký HS256, decoder chỉ chấp nhận HS512.
        String token = sign(secret, JWSAlgorithm.HS256,
                claims(UUID.randomUUID().toString(), Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThrows(BadJwtException.class, () -> decoder.decode(token));
        verify(tokenExpiredRepository, never()).existsById(anyString());
    }

    @Test
    void decode_malformedToken_throwsBadJwtException() {
        assertThrows(BadJwtException.class, () -> decoder.decode("not-a-jwt"));
        verify(tokenExpiredRepository, never()).existsById(anyString());
    }

    // ===== Helpers =====

    private static byte[] randomKey() {
        byte[] key = new byte[64]; // HS512 cần key >= 512 bit
        new SecureRandom().nextBytes(key);
        return key;
    }

    private static JWTClaimsSet claims(String jti, Instant expiresAt) {
        Instant issuedAt = expiresAt.minus(2, ChronoUnit.HOURS);
        return new JWTClaimsSet.Builder()
                .subject("alice")
                .jwtID(jti)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("scope", "USER")
                .build();
    }

    private static String sign(byte[] key, JWSAlgorithm alg, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(alg), claims);
        jwt.sign(new MACSigner(key));
        return jwt.serialize();
    }
}
