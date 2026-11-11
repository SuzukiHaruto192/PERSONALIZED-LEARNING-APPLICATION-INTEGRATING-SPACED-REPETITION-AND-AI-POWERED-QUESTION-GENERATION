package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.dto.request.Quiz.QuizSearchRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quizzes")
@Tag(name = "Quiz", description = "API CRUD thông tin chung của quiz (chỉ quiz do user hiện tại tạo)")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizController {
    static final String REQUEST_EXAMPLE = """
            {
              "title": "Present Simple - Daily routines",
              "description": "Luyện thì hiện tại đơn qua chủ đề sinh hoạt hằng ngày",
              "skillId": 1,
              "topicId": 12,
              "customTopic": null,
              "formatId": 3,
              "difficulty": 2
            }
            """;

    static final String RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 1,
                "title": "Present Simple - Daily routines",
                "description": "Luyện thì hiện tại đơn qua chủ đề sinh hoạt hằng ngày",
                "skill": { "id": 1, "name": "Listening" },
                "topic": { "id": 12, "name": "Sinh hoạt hằng ngày" },
                "customTopic": null,
                "format": { "id": 3, "name": "Thông báo" },
                "difficulty": 2,
                "status": "READY",
                "createdBy": 7,
                "createdAt": "2026-11-10T08:00:00Z",
                "questionCount": 0
              },
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String DETAIL_RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 1,
                "title": "Present Simple - Daily routines",
                "description": "Luyện thì hiện tại đơn qua chủ đề sinh hoạt hằng ngày",
                "skill": { "id": 2, "name": "Reading" },
                "topic": { "id": 12, "name": "Sinh hoạt hằng ngày" },
                "customTopic": null,
                "format": { "id": 5, "name": "Email" },
                "difficulty": 2,
                "status": "READY",
                "createdBy": 7,
                "createdAt": "2026-11-10T08:00:00Z",
                "questionCount": 3,
                "passages": [
                  {
                    "id": 7,
                    "quizId": 1,
                    "position": 1,
                    "content": "Tom gets up at 6 a.m. every day. He ...",
                    "audioUrl": null,
                    "questionCount": 3
                  }
                ]
              },
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;


    static final String BODY_BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai validation của body: code = 1006 (INVALID_INPUT). Đọc map `errors` (key = field, \
            value = tên ErrorCode, mỗi field chỉ giữ lỗi đầu tiên):
            - title: QUIZ_TITLE_BLANK, QUIZ_TITLE_SIZE (key titleSizeValid, quá 200 ký tự sau trim)
            - skillId: SKILL_BLANK
            - customTopic: CUSTOM_TOPIC_SIZE
            - difficulty: QUIZ_DIFFICULTY_BLANK, QUIZ_DIFFICULTY_OUT_OF_RANGE
            - exactlyOneTopic: QUIZ_TOPIC_EXACTLY_ONE (có cả hoặc thiếu cả topicId và customTopic)

            2. Lỗi nghiệp vụ: không có `errors`, phân biệt bằng `code`:
            - 1071 QUIZ_SKILL_INVALID: skill không tồn tại
            - 1072 QUIZ_SKILL_TOPIC_MISMATCH: cặp (skillId, topicId) không có trong skill_topics
            - 1073 QUIZ_FORMAT_INVALID: format không tồn tại hoặc không thuộc skill

            Body không phải JSON hợp lệ hoặc sai kiểu field: code = 1060 (MALFORMED_REQUEST_BODY).
            """;

    static final String BODY_VALIDATION_ERROR_EXAMPLE = """
            {
              "code": 1006,
              "message": "Dữ liệu đầu vào không hợp lệ",
              "errors": {
                "title": "QUIZ_TITLE_BLANK",
                "difficulty": "QUIZ_DIFFICULTY_OUT_OF_RANGE",
                "exactlyOneTopic": "QUIZ_TOPIC_EXACTLY_ONE"
              },
              "path": "/api/v1/quizzes",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String BUSINESS_ERROR_EXAMPLE = """
            {
              "code": 1072,
              "message": "Topic không thuộc skill đã chọn",
              "path": "/api/v1/quizzes",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;


    static final String SEARCH_BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai query param: code = 1006 (INVALID_INPUT). Đọc map `errors`:
            - page: PAGE_INVALID (page < 0)
            - size: PAGE_SIZE_INVALID (ngoài 1-100)
            - difficulty: QUIZ_DIFFICULTY_OUT_OF_RANGE
            - status, skillId, ...: sai kiểu (vd status=ABC) thì value là thông báo lỗi chuyển kiểu

            2. sort sai định dạng hoặc field không được phép: code = 1076 (QUIZ_SORT_INVALID), không có `errors`.
            """;

    static final String SEARCH_VALIDATION_ERROR_EXAMPLE = """
            {
              "code": 1006,
              "message": "Dữ liệu đầu vào không hợp lệ",
              "errors": { "size": "PAGE_SIZE_INVALID", "page": "PAGE_INVALID" },
              "path": "/api/v1/quizzes",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String SORT_ERROR_EXAMPLE = """
            {
              "code": 1076,
              "message": "sort không hợp lệ, dạng field,asc|desc với field là createdAt, title, difficulty",
              "path": "/api/v1/quizzes",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    QuizService quizService;

    @PostMapping
    @Operation(
            summary = "Tạo quiz",
            description = "Tạo thông tin chung của quiz. Server tự gán createdBy, createdAt, status = READY.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = BODY_BAD_REQUEST_DESCRIPTION,
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(
                                                    name = "Sai validation",
                                                    value = BODY_VALIDATION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Lỗi nghiệp vụ", value = BUSINESS_ERROR_EXAMPLE)
                                        }))
            })
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @AuthenticationPrincipal CustomUserDetail userDetail, @Validated @RequestBody QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(quizService.createQuiz(userDetail.getId(), request)));
    }

    @GetMapping
    @Operation(
            summary = "Danh sách quiz của tôi",
            description = "Phân trang + lọc. keyword tìm trong title, không phân biệt hoa thường. "
                    + "sort dạng field,asc|desc với field là createdAt, title, difficulty.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = SEARCH_BAD_REQUEST_DESCRIPTION,
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(
                                                    name = "Sai query param",
                                                    value = SEARCH_VALIDATION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Sai sort", value = SORT_ERROR_EXAMPLE)
                                        }))
            })
    public ResponseEntity<ApiResponse<PageResponse<QuizResponse>>> getQuizzes(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @ParameterObject @Validated @ModelAttribute QuizSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getMyQuizzes(userDetail.getId(), request)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Chi tiết quiz",
            description = "Kèm mảng passages sắp theo position. "
                    + "Quiz không phải của user hiện tại trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = DETAIL_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getQuiz(userDetail.getId(), id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Sửa thông tin chung của quiz",
            description = "Chỉ người tạo được sửa. Quiz đang GENERATING không được sửa.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = BODY_BAD_REQUEST_DESCRIPTION,
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(
                                                    name = "Sai validation",
                                                    value = BODY_VALIDATION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Lỗi nghiệp vụ", value = BUSINESS_ERROR_EXAMPLE)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_GENERATING")
            })
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long id,
            @Validated @RequestBody QuizRequest request) {
        return ResponseEntity.ok(ApiResponse.success(quizService.updateQuiz(userDetail.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xoá quiz",
            description = "Xoá quiz kèm câu hỏi, đáp án, passage (flashcard tham chiếu passage được giữ lại). "
                    + "Chỉ người tạo được xoá.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS / QUIZ_GENERATING")
            })
    public ResponseEntity<Void> deleteQuiz(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        quizService.deleteQuiz(userDetail.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
