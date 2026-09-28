package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.ApiResponse;
import backend.dev.PLA.dto.LoginRequest;
import backend.dev.PLA.dto.SignupRequest;
import backend.dev.PLA.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<Object> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
    }
    @PostMapping("/signup")
    public ApiResponse<Object> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return authenticationService.signup(signupRequest);
    }
    @GetMapping("/hello")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Object> hello() {
        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("Hello World")
                .build();
    }
}
