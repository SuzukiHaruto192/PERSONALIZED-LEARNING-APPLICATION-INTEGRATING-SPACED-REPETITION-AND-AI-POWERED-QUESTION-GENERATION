package backend.dev.PLA.security;

import backend.dev.PLA.repositories.auth.TokenExpiredRepository;
import com.nimbusds.jose.util.Base64;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Decode JWT cho mỗi request có header {@code Authorization: Bearer ...}.
 *
 * <ol>
 *   <li>Nimbus verify chữ ký (HMAC) và hạn token ({@code exp}) — chỉ verify một lần.
 *   <li>Kiểm tra {@code jti} có nằm trong blacklist {@code token_expired} (token đã logout) không.
 * </ol>
 */
@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder nimbusJwtDecoder;
    private final TokenExpiredRepository tokenExpiredRepository;

    public CustomJwtDecoder(
            @Value("${ldq.jwt.base64-secret}") String jwtKey, TokenExpiredRepository tokenExpiredRepository) {
        this.tokenExpiredRepository = tokenExpiredRepository;
        this.nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey(jwtKey))
                .macAlgorithm(SecurityConfiguration.JWT_ALGORITHM)
                .build();
        // Mặc định Nimbus cho lệch giờ 60s; đặt 0 để token hết hạn đúng lúc như validate cũ.
        this.nimbusJwtDecoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = nimbusJwtDecoder.decode(token);

        String jti = jwt.getId();
        if (jti == null || tokenExpiredRepository.existsById(jti)) {
            throw new BadJwtException("Token không hợp lệ hoặc đã đăng xuất");
        }
        return jwt;
    }

    private static SecretKey getSecretKey(String jwtKey) {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityConfiguration.JWT_ALGORITHM.getName());
    }
}
