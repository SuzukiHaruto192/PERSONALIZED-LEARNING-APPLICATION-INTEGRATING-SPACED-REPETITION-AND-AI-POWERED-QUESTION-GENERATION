package backend.dev.PLA.services.auth;

import backend.dev.PLA.dto.request.auth.LoginRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.entities.auth.TokenExpired;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.auth.TokenExpiredRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.security.JwtService;
import com.nimbusds.jose.JOSEException;
import java.text.ParseException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    JwtService jwtService;
    TokenExpiredRepository tokenExpiredRepository;

    public ApiResponse<String> getToken(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        String token = jwtService.createAccessToken(authentication);

        return ApiResponse.<String>builder()
                .message("login successfully!")
                .data(token)
                .build();
    }

    public ApiResponse<Boolean> verifyToken(String token) throws ParseException, JOSEException {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        jwtService.validateToken(token);

        return ApiResponse.<Boolean>builder().message("true").build();
    }

    @Transactional
    public void logout(Jwt jwt) {
        String jti = jwt.getId();
        if (tokenExpiredRepository.existsById(jti)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        tokenExpiredRepository.save(new TokenExpired(jti, jwt.getExpiresAt()));
    }
}
