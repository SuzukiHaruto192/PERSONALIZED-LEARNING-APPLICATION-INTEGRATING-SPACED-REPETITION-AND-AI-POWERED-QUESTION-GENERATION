package backend.dev.PLA.security;

import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.TokenExpiredRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${ldq.jwt.base64-secret}")
    private String jwtKey;

    private final JwtService jwtService;
    private final TokenExpiredRepository tokenExpiredRepository;
    private NimbusJwtDecoder nimbusJwtDecoder = null;

    public Jwt decode(String token) throws JwtException {
        // 1. Gọi hàm validate tự viết của bạn trước
        try {
            jwtService.validateToken(token);
        } catch (JOSEException | ParseException | AppException e) {
            throw new BadJwtException(e.getMessage());
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                    .macAlgorithm(SecurityConfiguration.JWT_ALGORITHM)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length,
                SecurityConfiguration.JWT_ALGORITHM.getName());
    }
}