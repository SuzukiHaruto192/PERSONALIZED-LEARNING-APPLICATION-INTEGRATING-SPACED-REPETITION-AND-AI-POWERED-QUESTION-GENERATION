package backend.dev.PLA.security;

import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.TokenExpiredRepository;
import backend.dev.PLA.security.SecurityConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final TokenExpiredRepository tokenExpiredRepository;

    @Value("${ldq.jwt.base64-secret}")
    private String jwtKey;

    @Value("${ldq.jwt.token-validity-in-seconds}")
    private long accessTokenExpiration;

    public String createAccessToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);
        String scope = "USER";
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .id(UUID.randomUUID().toString())
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(SecurityConfiguration.JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
    public void validateToken(String token) throws JOSEException, ParseException {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        SecretKeySpec sks = new SecretKeySpec(keyBytes, 0, keyBytes.length,
                SecurityConfiguration.JWT_ALGORITHM.getName());

        SignedJWT signedJWT = SignedJWT.parse(token);

        JWSVerifier verifier = new MACVerifier(sks);

        boolean verified = signedJWT.verify(verifier);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();



        if(expiryTime.before(new Date())
                ||!verified
                ||tokenExpiredRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())
        ) {
           throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}