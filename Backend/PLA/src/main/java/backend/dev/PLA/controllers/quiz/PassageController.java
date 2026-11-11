package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.PassageRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.PassageResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.PassageService;
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
@Tag(name = "Passage", description = "API CRUD đoạn văn / audio dùng chung cho nhiều câu hỏi trong quiz")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageController {
    static final String REQUEST_EXAMPLE = """
            {
              "position": null,
              "content": "Tom gets up at 6 a.m. every day. He ...",
              "audioUrl": "https://cdn.example.com/audio/routine-01.mp3"
            }
            """;

    static final String RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 7,
                "quizId": 1,
                "position": 1,
                "content": "Tom gets up at 6 a.m. every day. He ...",
                "audioUrl": "https://cdn.example.com/audio/routine-01.mp3",
                "questionCount": 0
              },
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String LIST_RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": [
                {
                  "id": 7,
                  "quizId": 1,
                  "position": 1,
                  "content": "Tom gets up at 6 a.m. every day. He ...",
                  "audioUrl": null,
                  "questionCount": 3
                },
                {
                  "id": 8,
                  "quizId": 1,
                  "position": 2,
                  "content": null,
                  "audioUrl": "https://cdn.example.com/audio/routine-02.mp3",
                  "questionCount": 2
                }
              ],
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai validation của body: code = 1006 (INVALID_INPUT). Đọc map `errors`:
            - contentOrAudioPresent: PASSAGE_CONTENT_REQUIRED (thiếu cả content và audioUrl)
            - audioUrlValid: PASSAGE_AUDIO_URL_INVALID (không phải URL http/https hoặc quá 500 ký tự)
            - position: PASSAGE_POSITION_INVALID (ngoài 1-32767)

            2. Lỗi nghiệp vụ: code = 1081 (PASSAGE_POSITION_DUPLICATED), position trùng passage khác trong quiz.

            Body không phải JSON hợp lệ hoặc sai kiểu field: code = 1060 (MALFORMED_REQUEST_BODY).
            """;

    static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "code": 1006,
              "message": "Dữ liệu đầu vào không hợp lệ",
              "errors": { "contentOrAudioPresent": "PASSAGE_CONTENT_REQUIRED" },
              "path": "/api/v1/quizzes/1/passages",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String POSITION_ERROR_EXAMPLE = """
            {
              "code": 1081,
              "message": "position đã được dùng bởi passage khác trong quiz",
              "path": "/api/v1/quizzes/1/passages",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    PassageService passageService;

    @PostMapping("/quizzes/{quizId}/passages")
    @Operation(
            summary = "Thêm passage vào quiz",
            description = "Phải có content hoặc audioUrl. position bỏ trống thì xếp cuối (max + 1). "
                    + "Chỉ người tạo quiz được thêm.",
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
                                            @ExampleObject(name = "Trùng position", value = POSITION_ERROR_EXAMPLE)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_GENERATING")
            })
    public ResponseEntity<ApiResponse<PassageResponse>> createPassage(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long quizId,
            @Validated @RequestBody PassageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(passageService.createPassage(userDetail.getId(), quizId, request)));
    }

    @GetMapping("/quizzes/{quizId}/passages")
    @Operation(
            summary = "Danh sách passage của quiz",
            description = "Sắp theo position. Quiz không phải của user hiện tại trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = LIST_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<List<PassageResponse>>> getPassages(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.success(passageService.getPassages(userDetail.getId(), quizId)));
    }

    @GetMapping("/passages/{id}")
    @Operation(
            summary = "Chi tiết passage",
            description = "Passage thuộc quiz của người khác trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "PASSAGE_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<PassageResponse>> getPassage(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(passageService.getPassage(userDetail.getId(), id)));
    }

    @PutMapping("/passages/{id}")
    @Operation(
            summary = "Sửa passage",
            description = "position bỏ trống thì giữ nguyên vị trí cũ. Chỉ người tạo quiz được sửa. "
                    + "Quiz đã có lượt làm bài vẫn sửa được.",
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
                        description = "PASSAGE_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_GENERATING")
            })
    public ResponseEntity<ApiResponse<PassageResponse>> updatePassage(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long id,
            @Validated @RequestBody PassageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(passageService.updatePassage(userDetail.getId(), id, request)));
    }

    @DeleteMapping("/passages/{id}")
    @Operation(
            summary = "Xoá passage",
            description = "Flashcard tham chiếu passage được giữ lại (source_passage_id = null). "
                    + "Không xoá được khi còn câu hỏi gắn vào hoặc quiz đã có lượt làm bài.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "PASSAGE_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "PASSAGE_HAS_QUESTIONS / QUIZ_HAS_ATTEMPTS / QUIZ_GENERATING")
            })
    public ResponseEntity<Void> deletePassage(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        passageService.deletePassage(userDetail.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
