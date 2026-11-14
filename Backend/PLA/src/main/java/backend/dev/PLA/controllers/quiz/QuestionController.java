package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.QuestionOrderRequest;
import backend.dev.PLA.dto.request.Quiz.QuestionRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Question", description = "API CRUD câu hỏi trong quiz (kèm options chỉ để đọc) và sắp xếp thứ tự câu")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionController {
    static final String REQUEST_EXAMPLE = """
            {
              "passageId": 7,
              "position": null,
              "type": "SINGLE_CHOICE",
              "content": "What time does Tom get up?",
              "correctText": null,
              "explanation": "Đoạn văn nói Tom dậy lúc 6 giờ.",
              "grammarTopicId": 40
            }
            """;

    static final String FILL_BLANK_REQUEST_EXAMPLE = """
            {
              "passageId": null,
              "position": null,
              "type": "FILL_BLANK",
              "content": "She ___ to school every day.",
              "correctText": "goes",
              "explanation": "Chủ ngữ số ít, thì hiện tại đơn thêm -es.",
              "grammarTopicId": 40
            }
            """;

    static final String RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 15,
                "quizId": 1,
                "passageId": 7,
                "position": 1,
                "type": "SINGLE_CHOICE",
                "content": "What time does Tom get up?",
                "correctText": null,
                "explanation": "Đoạn văn nói Tom dậy lúc 6 giờ.",
                "grammarTopic": { "id": 40, "name": "Thì hiện tại đơn" },
                "options": [
                  { "id": 31, "questionId": 15, "position": 1, "content": "6 a.m.", "isCorrect": true },
                  { "id": 32, "questionId": 15, "position": 2, "content": "7 a.m.", "isCorrect": false }
                ],
                "isComplete": true
              },
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String LIST_RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": [
                {
                  "id": 15,
                  "quizId": 1,
                  "passageId": 7,
                  "position": 1,
                  "type": "SINGLE_CHOICE",
                  "content": "What time does Tom get up?",
                  "correctText": null,
                  "explanation": null,
                  "grammarTopic": null,
                  "options": [
                    { "id": 31, "questionId": 15, "position": 1, "content": "6 a.m.", "isCorrect": true }
                  ],
                  "isComplete": false
                },
                {
                  "id": 16,
                  "quizId": 1,
                  "passageId": null,
                  "position": 2,
                  "type": "FILL_BLANK",
                  "content": "She ___ to school every day.",
                  "correctText": "goes",
                  "explanation": null,
                  "grammarTopic": { "id": 40, "name": "Thì hiện tại đơn" },
                  "options": [],
                  "isComplete": true
                }
              ],
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String ORDER_REQUEST_EXAMPLE = """
            { "questionIds": [16, 15] }
            """;

    static final String BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai validation của body: code = 1006 (INVALID_INPUT). Đọc map `errors`:
            - type: QUESTION_TYPE_BLANK
            - content: QUESTION_CONTENT_BLANK
            - position: QUESTION_POSITION_INVALID (ngoài 1-32767)
            - correctTextValid: QUESTION_CORRECT_TEXT_INVALID (FILL_BLANK thiếu correctText / SINGLE_CHOICE có correctText)
            - correctTextSizeValid: CORRECT_TEXT_SIZE (quá 255 ký tự sau trim)

            2. Lỗi nghiệp vụ: không có `errors`, phân biệt bằng `code`:
            - 1088 QUESTION_POSITION_DUPLICATED: position trùng câu khác trong quiz
            - 1090 PASSAGE_NOT_IN_QUIZ: passageId không tồn tại hoặc thuộc quiz khác
            - 1091 QUESTION_GRAMMAR_TOPIC_INVALID: grammarTopicId không tồn tại hoặc không thuộc nhóm GRAMMAR
            - 1092 QUIZ_QUESTION_LIMIT: quiz đã có 100 câu (chỉ khi POST)

            Body không phải JSON hợp lệ, sai kiểu field hoặc type không phải SINGLE_CHOICE / FILL_BLANK: \
            code = 1060 (MALFORMED_REQUEST_BODY).
            """;

    static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "code": 1006,
              "message": "Dữ liệu đầu vào không hợp lệ",
              "errors": { "correctTextValid": "QUESTION_CORRECT_TEXT_INVALID" },
              "path": "/api/v1/quizzes/1/questions",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String POSITION_ERROR_EXAMPLE = """
            {
              "code": 1088,
              "message": "position đã được dùng bởi câu hỏi khác trong quiz",
              "path": "/api/v1/quizzes/1/questions",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String PASSAGE_ERROR_EXAMPLE = """
            {
              "code": 1090,
              "message": "Passage không thuộc quiz này",
              "path": "/api/v1/quizzes/1/questions",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String ORDER_ERROR_EXAMPLE = """
            {
              "code": 1094,
              "message": "questionIds phải chứa đúng và đủ id câu hỏi của quiz, không trùng",
              "path": "/api/v1/quizzes/1/questions/order",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String HAS_ATTEMPTS_ERROR_EXAMPLE = """
            {
              "code": 1068,
              "message": "Quiz đã có lượt làm bài, không thể đổi loại câu hoặc đáp án",
              "path": "/api/v1/questions/15",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    static final String HAS_OPTIONS_ERROR_EXAMPLE = """
            {
              "code": 1093,
              "message": "Câu hỏi đang có đáp án, phải xoá đáp án trước khi đổi sang FILL_BLANK",
              "path": "/api/v1/questions/15",
              "timestamp": "2026-11-10T08:00:00Z"
            }
            """;

    QuestionService questionService;

    @PostMapping("/quizzes/{quizId}/questions")
    @Operation(
            summary = "Thêm câu hỏi vào quiz",
            description = "position bỏ trống thì xếp cuối (max + 1). Mỗi quiz tối đa 100 câu. "
                    + "Chỉ người tạo quiz được thêm; quiz đã có lượt làm bài vẫn thêm được.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            examples = {
                                                @ExampleObject(name = "SINGLE_CHOICE", value = REQUEST_EXAMPLE),
                                                @ExampleObject(name = "FILL_BLANK", value = FILL_BLANK_REQUEST_EXAMPLE)
                                            })),
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
                                            @ExampleObject(name = "Passage khác quiz", value = PASSAGE_ERROR_EXAMPLE)
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
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long quizId,
            @Validated @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(questionService.createQuestion(userDetail.getId(), quizId, request)));
    }

    @GetMapping("/quizzes/{quizId}/questions")
    @Operation(
            summary = "Danh sách câu hỏi của quiz",
            description = "Sắp theo position, mỗi câu kèm options. Có passageId thì chỉ lấy câu của passage đó "
                    + "(passage không thuộc quiz → 400 PASSAGE_NOT_IN_QUIZ). "
                    + "Quiz không phải của user hiện tại trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = LIST_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "PASSAGE_NOT_IN_QUIZ",
                        content = @Content(examples = @ExampleObject(value = PASSAGE_ERROR_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long quizId,
            @Parameter(description = "Lọc theo passage của quiz", example = "7") @RequestParam(required = false)
                    Long passageId) {
        return ResponseEntity.ok(
                ApiResponse.success(questionService.getQuestions(userDetail.getId(), quizId, passageId)));
    }

    @GetMapping("/questions/{id}")
    @Operation(
            summary = "Chi tiết câu hỏi",
            description =
                    "Kèm options sắp theo position. Câu thuộc quiz của người khác trả 403, không tồn tại trả 404.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUESTION_NOT_FOUND")
            })
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestion(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(questionService.getQuestion(userDetail.getId(), id)));
    }

    @PutMapping("/questions/{id}")
    @Operation(
            summary = "Sửa câu hỏi",
            description = "position bỏ trống thì giữ nguyên vị trí cũ; passageId null thì gỡ câu khỏi passage. "
                    + "Quiz đã có lượt làm bài: sửa được content, explanation, grammarTopicId, passageId, position; "
                    + "không đổi được type, correctText (409 QUIZ_HAS_ATTEMPTS). "
                    + "Đổi sang FILL_BLANK khi câu còn options → 409 QUESTION_HAS_OPTIONS.",
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
                                            @ExampleObject(name = "Trùng position", value = POSITION_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Passage khác quiz", value = PASSAGE_ERROR_EXAMPLE)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUESTION_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS / QUESTION_HAS_OPTIONS / QUIZ_GENERATING",
                        content =
                                @Content(
                                        examples = {
                                            @ExampleObject(
                                                    name = "Có lượt làm bài",
                                                    value = HAS_ATTEMPTS_ERROR_EXAMPLE),
                                            @ExampleObject(name = "Còn options", value = HAS_OPTIONS_ERROR_EXAMPLE)
                                        }))
            })
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long id,
            @Validated @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(questionService.updateQuestion(userDetail.getId(), id, request)));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(
            summary = "Xoá câu hỏi",
            description = "Xoá kèm options. Không đánh lại position các câu còn lại. "
                    + "Không xoá được khi quiz đã có lượt làm bài.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUESTION_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_HAS_ATTEMPTS / QUIZ_GENERATING")
            })
    public ResponseEntity<Void> deleteQuestion(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        questionService.deleteQuestion(userDetail.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/quizzes/{quizId}/questions/order")
    @Operation(
            summary = "Sắp xếp lại thứ tự câu hỏi",
            description = "questionIds phải chứa đúng và đủ id mọi câu của quiz, không trùng. "
                    + "Câu đầu danh sách có position = 1, tiếp theo 2, 3... Trả danh sách câu theo thứ tự mới. "
                    + "Quiz đã có lượt làm bài vẫn sắp xếp được.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = ORDER_REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        content = @Content(examples = @ExampleObject(value = LIST_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "QUESTION_ORDER_INVALID (thiếu, thừa hoặc trùng id); "
                                + "thiếu questionIds → 1006 với errors.questionIds = QUESTION_ORDER_INVALID",
                        content =
                                @Content(
                                        examples =
                                                @ExampleObject(name = "Sai danh sách", value = ORDER_ERROR_EXAMPLE))),
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
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> reorderQuestions(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long quizId,
            @Validated @RequestBody QuestionOrderRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(questionService.reorderQuestions(userDetail.getId(), quizId, request)));
    }
}
