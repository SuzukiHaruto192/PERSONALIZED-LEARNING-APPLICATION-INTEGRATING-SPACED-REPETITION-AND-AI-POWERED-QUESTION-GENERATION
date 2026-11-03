package backend.dev.PLA.exceptions;

import backend.dev.PLA.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Kế thừa {@link ResponseEntityExceptionHandler} để mọi exception chuẩn của Spring MVC
 * (sai method, sai content-type, sai kiểu tham số, JSON hỏng, URL không tồn tại, ...)
 * được trả về đúng HTTP status thay vì rơi vào handler chung (500).
 * <p>
 * Mọi handler (kể cả lỗi nghiệp vụ) đều trả {@code ResponseEntity<Object>} và đi qua
 * {@link #handleExceptionInternal}, nên body lỗi luôn là {@link ApiResponse} cùng một định dạng.
 */
@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ===== Lỗi nghiệp vụ & security =====

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppException(AppException ex, WebRequest request) {
        return handleErrorCode(ex, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        ErrorCode errorCode = ErrorCode.BAD_CREDENTIALS;
        return handleErrorCode(ex, errorCode, errorCode.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return handleErrorCode(ex, errorCode, errorCode.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncategorizedException(Exception ex, WebRequest request) {
        log.error("Unhandled exception at [{}]: ", requestUri(request), ex);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return handleErrorCode(ex, errorCode, errorCode.getMessage(), request);
    }

    // ===== Lỗi chuẩn của Spring MVC (override để trả chi tiết hơn) =====

    /**
     * Ném khi body {@code @RequestBody} có {@code @Valid}/{@code @Validated} vi phạm constraint
     * (@NotBlank, @Size, @Pattern, ...). JSON đã parse thành công, chỉ là dữ liệu không hợp lệ.
     * VD: POST /topics với {@code "slug":"Sân Bay"} hoặc thiếu {@code nameVi}.
     * Trả 400 kèm {@code errors} theo từng field.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiResponse<Void> body = buildBody(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors, request);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Ném khi thiếu {@code @RequestParam} bắt buộc ({@code required = true}, không có {@code defaultValue}),
     * hoặc có tên nhưng giá trị rỗng (vd {@code ?active=}). Spring ném ngay lúc resolve tham số,
     * method controller không được gọi. Trả 400 kèm tên tham số bị thiếu.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = Map.of(
                ex.getParameterName(), "Thiếu tham số bắt buộc '" + ex.getParameterName() + "'");
        ApiResponse<Void> body = buildBody(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors, request);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Ném khi không convert được giá trị {@code @PathVariable}/{@code @RequestParam} sang kiểu khai báo
     * (thực tế là {@link MethodArgumentTypeMismatchException}).
     * VD: {@code GET /topics/abc} (id là Long), {@code ?active=abc} (boolean).
     * Trả 400 kèm tên tham số, giá trị sai và kiểu mong đợi.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        Map<String, String> errors = null;
        if (name != null) {
            String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "hợp lệ";
            errors = Map.of(name, "Giá trị '" + ex.getValue() + "' không đúng kiểu " + expected);
        }
        ApiResponse<Void> body = buildBody(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors, request);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Ném khi Jackson không đọc được body thành object {@code @RequestBody} (xảy ra trước bước validate):
     * JSON sai cú pháp, thiếu body, giá trị enum không tồn tại ({@code "level":"Z1"}),
     * hoặc sai kiểu field ({@code "idTopicGroup":"abc"}). Trả 400 với MALFORMED_REQUEST_BODY.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = ErrorCode.MALFORMED_REQUEST_BODY;
        ApiResponse<Void> body = buildBody(errorCode, errorCode.getMessage(), null, request);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Điểm cuối chung của mọi handler trong class này.
     * <ul>
     *   <li>Nếu body đã là {@link ApiResponse} (do các handler ở trên tạo) thì giữ nguyên.</li>
     *   <li>Nếu không (các exception Spring MVC không override riêng) thì tạo {@link ApiResponse}
     *       từ status Spring đã chọn. Các lỗi thường gặp:
     *     <ul>
     *       <li>405 {@code HttpRequestMethodNotSupportedException}: URL đúng nhưng sai HTTP method
     *           (vd {@code DELETE /topics}); header {@code Allow} liệt kê method hợp lệ.</li>
     *       <li>415 {@code HttpMediaTypeNotSupportedException}: Content-Type của body không được hỗ trợ
     *           (vd gửi {@code text/plain} thay vì {@code application/json}).</li>
     *       <li>404 {@code NoResourceFoundException}: không có controller hay static resource nào khớp URL.</li>
     *       <li>406 {@code HttpMediaTypeNotAcceptableException}: header {@code Accept} yêu cầu định dạng server không trả được.</li>
     *     </ul>
     *   </li>
     * </ul>
     * Sau đó gọi {@code super} để Spring xử lý phần chuẩn (bỏ qua nếu response đã commit,
     * đánh dấu exception cho lỗi 500, giữ headers như {@code Allow}).
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (!(body instanceof ApiResponse<?>)) {
            ErrorCode errorCode = errorCodeFor(statusCode);
            body = buildBody(errorCode, errorCode.getMessage(), null, request);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ===== Helpers =====

    private ResponseEntity<Object> handleErrorCode(
            Exception ex, ErrorCode errorCode, String message, WebRequest request) {
        ApiResponse<Void> body = buildBody(errorCode, message, null, request);
        return handleExceptionInternal(ex, body, new HttpHeaders(), errorCode.getStatusCode(), request);
    }

    private ErrorCode errorCodeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> ErrorCode.ENDPOINT_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is4xxClientError() ? ErrorCode.INVALID_INPUT : ErrorCode.UNCATEGORIZED_EXCEPTION;
        };
    }

    private ApiResponse<Void> buildBody(
            ErrorCode errorCode, String message, Map<String, String> errors, WebRequest request) {
        return ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .errors(errors)
                .path(requestUri(request))
                .build();
    }

    private String requestUri(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : null;
    }
}
