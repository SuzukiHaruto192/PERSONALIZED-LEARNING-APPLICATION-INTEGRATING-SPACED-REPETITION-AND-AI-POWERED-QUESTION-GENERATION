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
@RequestMapping("/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
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
