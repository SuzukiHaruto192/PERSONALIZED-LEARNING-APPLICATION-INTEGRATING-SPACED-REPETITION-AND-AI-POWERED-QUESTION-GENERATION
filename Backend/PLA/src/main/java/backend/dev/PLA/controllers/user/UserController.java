package backend.dev.PLA.controllers.user;

import backend.dev.PLA.dto.request.auth.InformationUserRequest;
import backend.dev.PLA.dto.request.flashcard.SignupRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping("")
    @Operation(summary = "sign up", description = "tạo 1 người dùng mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return ApiResponse.<UserResponse>builder()
                .message("signup successfully!")
                .data(userService.createNewUser(signupRequest))
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "chỉnh sửa thông tin người dùng",
            description = "người dùng tự chỉnh sửa thông tin của chính mình ")
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @Parameter(hidden = true) @PathVariable Long id,
            @Valid @RequestBody InformationUserRequest informationUserRequest) {
        userService.updateUser(userDetail, informationUserRequest, id);
        return ApiResponse.<Void>builder()
                .message("update user detail successfully!")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(
            @AuthenticationPrincipal CustomUserDetail userDetail, @Parameter(hidden = true) @PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getUser(userDetail, id))
                .build();
    }
}
