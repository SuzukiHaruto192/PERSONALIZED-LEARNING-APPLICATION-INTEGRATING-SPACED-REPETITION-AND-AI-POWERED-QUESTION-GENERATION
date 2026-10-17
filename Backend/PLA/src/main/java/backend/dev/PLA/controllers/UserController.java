package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.request.InformationUserRequest;
import backend.dev.PLA.dto.request.SignupRequest;
import backend.dev.PLA.dto.response.UserResponse;
import backend.dev.PLA.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return ApiResponse.<UserResponse>builder()
                .message("signup successfully!")
                .data(userService.createNewUser(signupRequest))
                .build();
    }
    @PatchMapping("/me")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody InformationUserRequest informationUserRequest) {
           userService.updateUser(informationUserRequest);
        return ApiResponse.<Void>builder()
                .message("update user detail successfully!")
                .build();
    }
    @GetMapping("/me")
    public ApiResponse<UserResponse> getUser() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getUser())
                .build();
    }

}
