package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.request.InformationUserRequest;
import backend.dev.PLA.dto.request.SignupRequest;
import backend.dev.PLA.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class UserController {
    private final UserService userService;
    @PostMapping("/users")
    public ApiResponse<Object> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return ApiResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message("signup successfully!")
                .data(userService.createNewUser(signupRequest))
                .build();
    }
    @PutMapping("/users/me")
    public ApiResponse<Object> updateProfile(@Valid @RequestBody InformationUserRequest informationUserRequest) {
           userService.updateUser(informationUserRequest);
        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("update user detail successfully!")
                .build();
    }
}
