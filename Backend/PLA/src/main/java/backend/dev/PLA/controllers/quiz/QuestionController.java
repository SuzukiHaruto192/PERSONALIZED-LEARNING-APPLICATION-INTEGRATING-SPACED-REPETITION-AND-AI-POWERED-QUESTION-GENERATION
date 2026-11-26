package backend.dev.PLA.controllers.quiz;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(
        name = "Question",
        description =
                "API xem / xoá câu hỏi của quiz (kèm options). Câu hỏi do AI sinh khi tạo quiz, không tạo / sửa tay")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionController {
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
                "options": [
                  { "id": 31, "questionId": 15, "position": 1, "content": "6 a.m." },
                  { "id": 32, "questionId": 15, "position": 2, "content": "7 a.m." }
                ]
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
                  "options": [
                    { "id": 31, "questionId": 15, "position": 1, "content": "6 a.m." },
                    { "id": 32, "questionId": 15, "position": 2, "content": "7 a.m." }
                  ]
                },
                {
                  "id": 16,
                  "quizId": 1,
                  "passageId": null,
                  "position": 2,
                  "type": "FILL_BLANK",
                  "content": "She ___ to school every day.",
                  "options": []
                }
              ],
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

    QuestionService questionService;

    @GetMapping("/quizzes/{quizId}/questions")
    @Operation(
            summary = "Danh sách câu hỏi của quiz",
            description = "Sắp theo position, mỗi câu kèm options. Có passageId thì chỉ lấy câu của passage đó "
                    + "(passage không thuộc quiz → 400 PASSAGE_NOT_IN_QUIZ). "
                    + "Luôn ẩn đáp án: không có correctText, explanation, isComplete, grammarTopic và isCorrect "
                    + "của option. Đáp án chỉ xem được trong response nộp bài và lịch sử lượt làm. "
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
            description = "Kèm options sắp theo position. Luôn ẩn đáp án: không có correctText, explanation, "
                    + "isComplete, grammarTopic và isCorrect của option. Đáp án chỉ xem được trong response nộp bài và lịch sử lượt làm. "
                    + "Câu thuộc quiz của người khác trả 403, không tồn tại trả 404.",
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
}
