package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.ApiResponse;
import backend.dev.PLA.dto.LoginRequest;
import backend.dev.PLA.dto.SignupRequest;
import backend.dev.PLA.services.AuthenticationService;
import backend.dev.PLA.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class UserController {
    private final UserService userService;
    @PostMapping("/users")
    public ApiResponse<Object> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return userService.createNewUser(signupRequest);
    }
//    @PutMapping("/users/me")
//    public ApiResponse<Object> signin(@Valid @RequestBody LoginRequest loginRequest) {
//
//    }
}
