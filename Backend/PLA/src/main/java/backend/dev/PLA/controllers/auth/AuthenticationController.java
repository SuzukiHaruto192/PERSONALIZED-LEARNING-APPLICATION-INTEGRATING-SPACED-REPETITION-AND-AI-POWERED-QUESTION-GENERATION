package backend.dev.PLA.controllers.auth;

import backend.dev.PLA.dto.request.auth.LoginRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API xác thực người dùng")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @Operation(summary = "Đăng nhập", description = "Xác thực tài khoản người dùng và trả về JWT token.")
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authenticationService.getToken(loginRequest);
    }

    @GetMapping("/hello")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Object> hello() {
        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("Hello World")
                .build();
    }

    //    @Operation(
    //            summary = "test token",
    //            description = "Xác thực token người dùng gửi "
    //    )
    //    @PostMapping("/checktoken")
    //    public ApiResponse<Boolean> checkToken(@Valid @RequestBody TokenRequest tokenRequest) throws
    // ParseException, JOSEException {
    //        return authenticationService.verifyToken(tokenRequest.getToken());
    //    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "đánh dấu token trong danh mục token hết hạn")
    public ApiResponse<Void> logout(JwtAuthenticationToken authentication) {
        authenticationService.logout(authentication.getToken());
        return ApiResponse.<Void>builder().message("ok").build();
    }
}
