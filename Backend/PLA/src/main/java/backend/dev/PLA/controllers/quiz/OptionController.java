package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.OptionRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Option", description = "API CRUD đáp án lựa chọn cho câu hỏi SINGLE_CHOICE")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OptionController {
    static final String REQUEST_EXAMPLE = """
            {
              "position": null,
              "content": "goes",
              "isCorrect": true
            }
            """;

    static final String RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 31,
                "questionId": 15,
                "position": 1,
                "content": "goes",
                "isCorrect": true
              },
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String LIST_RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": [
                { "id": 31, "questionId": 15, "position": 1, "content": "goes", "isCorrect": true },
                { "id": 32, "questionId": 15, "position": 2, "content": "go", "isCorrect": false }
              ],
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai validation của body: code = 1006 (INVALID_INPUT). Đọc map `errors`:
            - content: OPTION_CONTENT_BLANK
            - isCorrect: ANSWER_BLANK
            - position: OPTION_POSITION_INVALID (ngoài 1-32767)

            2. Lỗi nghiệp vụ: không có `errors`, phân biệt bằng `code`:
            - 1098 OPTION_POSITION_DUPLICATED: position trùng đáp án khác của câu
            - 1096 OPTION_NOT_ALLOWED: câu hỏi là FILL_BLANK (chỉ khi POST)
            - 1099 QUESTION_OPTION_LIMIT: câu đã có 6 đáp án (chỉ khi POST)

            Body không phải JSON hợp lệ hoặc sai kiểu field: code = 1060 (MALFORMED_REQUEST_BODY).
            """;

    static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "code": 1006,
              "message": "Dữ liệu đầu vào không hợp lệ",
              "errors": { "content": "OPTION_CONTENT_BLANK", "isCorrect": "ANSWER_BLANK" },
              "path": "/api/v1/questions/15/options",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String POSITION_ERROR_EXAMPLE = """
            {
              "code": 1098,
              "message": "position đã được dùng bởi đáp án khác của câu",
              "path": "/api/v1/questions/15/options",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String LIMIT_ERROR_EXAMPLE = """
            {
              "code": 1099,
              "message": "Một câu hỏi tối đa 6 đáp án",
              "path": "/api/v1/questions/15/options",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String HAS_ATTEMPTS_ERROR_EXAMPLE = """
            {
              "code": 1068,
              "message": "Quiz đã có lượt làm bài, không thể thêm, xoá đáp án hoặc đổi đáp án đúng",
              "path": "/api/v1/options/31",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    OptionService optionService;

    @PostMapping("/questions/{questionId}/options")
    @Operation(
            summary = "Thêm đáp án cho câu hỏi",
            description = "Chỉ câu SINGLE_CHOICE, tối đa 6 đáp án. position bỏ trống thì xếp cuối (max + 1). "
                    + "isCorrect = true thì các đáp án khác của câu tự về false. "
                    + "Quiz đã có lượt làm bài không thêm được.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = BAD_REQUEST_DESCRIPTION,
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(name = "Sai validation", value = VALIDATION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Trùng position", value = POSITION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Quá 6 đáp án", value = LIMIT_ERROR_EXAMPLE)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUESTION_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS / QUIZ_GENERATING",
                        content = @Content(examples = @ExampleObject(value = HAS_ATTEMPTS_ERROR_EXAMPLE)))
            })
    public ResponseEntity<ApiResponse<OptionResponse>> createOption(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long questionId,
            @Validated @RequestBody OptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(optionService.createOption(userDetail.getId(), questionId, request)));
    }

    @GetMapping("/questions/{questionId}/options")
    @Operation(
            summary = "Danh sách đáp án của câu hỏi",
            description = "Sắp theo position. Câu thuộc quiz của người khác trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = LIST_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUESTION_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<List<OptionResponse>>> getOptions(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long questionId) {
        return ResponseEntity.ok(ApiResponse.success(optionService.getOptions(userDetail.getId(), questionId)));
    }

    @GetMapping("/options/{id}")
    @Operation(
            summary = "Chi tiết đáp án",
            description = "Đáp án thuộc quiz của người khác trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "OPTION_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<OptionResponse>> getOption(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(optionService.getOption(userDetail.getId(), id)));
    }

    @PutMapping("/options/{id}")
    @Operation(
            summary = "Sửa đáp án",
            description = "position bỏ trống thì giữ nguyên vị trí cũ. isCorrect = true thì các đáp án khác của câu "
                    + "tự về false. Quiz đã có lượt làm bài: sửa được content, position; không đổi được isCorrect.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = BAD_REQUEST_DESCRIPTION,
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(name = "Sai validation", value = VALIDATION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Trùng position", value = POSITION_ERROR_EXAMPLE)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "OPTION_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS (đổi isCorrect) / QUIZ_GENERATING",
                        content = @Content(examples = @ExampleObject(value = HAS_ATTEMPTS_ERROR_EXAMPLE)))
            })
    public ResponseEntity<ApiResponse<OptionResponse>> updateOption(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long id,
            @Validated @RequestBody OptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(optionService.updateOption(userDetail.getId(), id, request)));
    }

    @DeleteMapping("/options/{id}")
    @Operation(
            summary = "Xoá đáp án",
            description = "Không đánh lại position các đáp án còn lại. Không xoá được khi quiz đã có lượt làm bài.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "OPTION_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS / QUIZ_GENERATING")
            })
    public ResponseEntity<Void> deleteOption(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        optionService.deleteOption(userDetail.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
