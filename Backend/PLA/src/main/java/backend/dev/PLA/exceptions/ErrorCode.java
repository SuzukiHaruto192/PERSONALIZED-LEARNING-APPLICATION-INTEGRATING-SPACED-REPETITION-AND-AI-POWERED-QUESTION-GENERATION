package backend.dev.PLA.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Auth & User Business Errors
    UNAUTHENTICATED(1001, "Token không đúng hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1002, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    USER_EXISTED(1003, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1004, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    BAD_CREDENTIALS(1005, "Sai tên đăng nhập hoặc mật khẩu!", HttpStatus.UNAUTHORIZED),

    // Validation Errors - General & Username
    INVALID_INPUT(1006, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    USERNAME_BLANK(1007, "Tên đăng nhập không được để trống", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID_SIZE(1008, "Tên đăng nhập phải từ 6 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID_PATTERN(1009, "Tên đăng nhập chỉ được chứa chữ cái và số", HttpStatus.BAD_REQUEST),

    // Validation Errors - Password
    PASSWORD_BLANK(1010, "Mật khẩu không được để trống", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_SIZE(1011, "Mật khẩu phải từ 6 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_PATTERN(1012, "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Validation Errors - Email
    EMAIL_BLANK(1013, "Email không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID_FORMAT(1014, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),

    // Validation Errors - Phone Number
    PHONE_BLANK(1015, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_INVALID_SIZE(1016, "Số điện thoại phải từ 10 đến 15 ký tự", HttpStatus.BAD_REQUEST),
    PHONE_INVALID_PATTERN(1017, "Số điện thoại chỉ được chứa chữ số", HttpStatus.BAD_REQUEST),

    // Duplicate Errors
    EMAIL_EXISTED(1018, "Email này đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1019, "Số điện thoại này đã được sử dụng", HttpStatus.BAD_REQUEST),

    // Validation Errors - Score (0.0 - 9.0)
    CURRENT_SCORE_MIN(1020, "Điểm hiện tại tối thiểu phải là 0.0", HttpStatus.BAD_REQUEST),
    CURRENT_SCORE_MAX(1021, "Điểm hiện tại tối đa không được vượt quá 9.0", HttpStatus.BAD_REQUEST),
    TARGET_SCORE_MIN(1022, "Điểm mục tiêu tối thiểu phải là 0.0", HttpStatus.BAD_REQUEST),
    TARGET_SCORE_MAX(1023, "Điểm mục tiêu tối đa không được vượt quá 9.0", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}