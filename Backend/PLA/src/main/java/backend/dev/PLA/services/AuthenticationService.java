package backend.dev.PLA.services;

import backend.dev.PLA.dto.ApiResponse;
import backend.dev.PLA.dto.LoginRequest;
import backend.dev.PLA.dto.SignupRequest;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.DataAlreadyExistException;
import backend.dev.PLA.exceptions.DataNotFoundException;
import backend.dev.PLA.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import backend.dev.PLA.security.JwtService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ApiResponse<String> login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
//        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtService.createAccessToken(authentication);

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("login successfully!")
                .data(token)
                .build();
    }
}
