package backend.dev.PLA.controllers.quiz;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(
        name = "Option",
        description = "API xem / xoá đáp án của câu SINGLE_CHOICE. Đáp án do AI sinh khi tạo quiz, không tạo / sửa tay")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OptionController {
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

    OptionService optionService;

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
