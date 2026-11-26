package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.AttemptRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptSearchRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.AttemptResponse;
import backend.dev.PLA.dto.response.quiz.QuizAttemptHistoryResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.QuizAttemptService;
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
@Tag(name = "Quiz attempt", description = "API lịch sử làm bài quiz của user hiện tại")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptController {
    static final String SUBMIT_REQUEST_EXAMPLE = """
            {
              "answers": [
                { "questionId": 15, "optionId": 31 },
                { "questionId": 16, "answerText": "goes" },
                { "questionId": 17 }
              ]
            }
            """;

    static final String ATTEMPT_RESPONSE_EXAMPLE = """
            {
              "code": 1000,
              "data": {
                "id": 3,
                "quiz": { "id": 1, "name": "Present Simple - Daily routines" },
                "skill": { "id": 2, "name": "Reading" },
                "startedAt": "2026-11-18T08:00:00Z",
                "submittedAt": "2026-11-18T08:00:00Z",
                "correctCount": 2,
                "totalQuestions": 3,
                "questions": [
                  {
                    "question": {
                      "id": 15, "quizId": 1, "passageId": null, "position": 1, "type": "SINGLE_CHOICE",
                      "content": "She ___ to school every day.", "correctText": null,
                      "explanation": "Chủ ngữ ngôi thứ 3 số ít nên động từ thêm -es", "grammarTopic": null,
                      "options": [
                        { "id": 30, "questionId": 15, "position": 1, "content": "go", "isCorrect": false },
                        { "id": 31, "questionId": 15, "position": 2, "content": "goes", "isCorrect": true }
                      ],
                      "isComplete": true
                    },
                    "answer": { "attemptId": 3, "questionId": 15, "selectedOptionId": 31, "answerText": null, "isCorrect": true }
                  },
                  {
                    "question": {
                      "id": 16, "quizId": 1, "passageId": null, "position": 2, "type": "FILL_BLANK",
                      "content": "He ___ (go) home at 6.", "correctText": "goes", "explanation": null,
                      "grammarTopic": null, "options": [], "isComplete": true
                    },
                    "answer": { "attemptId": 3, "questionId": 16, "selectedOptionId": null, "answerText": "goes", "isCorrect": true }
                  },
                  {
                    "question": {
                      "id": 17, "quizId": 1, "passageId": null, "position": 3, "type": "SINGLE_CHOICE",
                      "content": "They ___ coffee.", "correctText": null, "explanation": null, "grammarTopic": null,
                      "options": [
                        { "id": 34, "questionId": 17, "position": 1, "content": "drinks", "isCorrect": false },
                        { "id": 35, "questionId": 17, "position": 2, "content": "drink", "isCorrect": true }
                      ],
                      "isComplete": true
                    },
                    "answer": null
                  }
                ]
              },
              "timestamp": "2026-11-18T08:00:00Z"
            }
            """;

    static final String SUBMIT_BAD_REQUEST_DESCRIPTION = """
            Có 2 dạng lỗi 400:

            1. Sai validation của body: code = 1006 (INVALID_INPUT). Đọc map `errors`:
            - answers: ATTEMPT_ANSWERS_EMPTY
            - answers[i].questionId: ATTEMPT_QUESTION_ID_BLANK
            - answers[i].answerText: ATTEMPT_ANSWER_TEXT_SIZE

            2. Lỗi nghiệp vụ: không có `errors`, phân biệt bằng `code`:
            - 1108 ATTEMPT_QUESTION_NOT_IN_QUIZ: questionId không thuộc quiz
            - 1109 ATTEMPT_QUESTION_DUPLICATED: một câu được trả lời 2 lần
            - 1110 ATTEMPT_OPTION_NOT_IN_QUESTION: optionId không thuộc câu hỏi
            - 1111 ATTEMPT_ANSWER_TYPE_MISMATCH: SINGLE_CHOICE gửi answerText hoặc FILL_BLANK gửi optionId
            """;

    QuizAttemptService quizAttemptService;

    @GetMapping("/attempts")
    @Operation(
            summary = "Tất cả lịch sử làm bài của tôi",
            description =
                    "Phân trang, lượt nộp mới nhất trước. Mỗi lượt chỉ gồm thông tin tổng quát (không có answers); "
                            + "xem chi tiết từng câu qua GET /api/v1/quizzes/{id}/attempts.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "code 1006, errors.page = PAGE_INVALID / errors.size = PAGE_SIZE_INVALID")
            })
    public ResponseEntity<ApiResponse<PageResponse<AttemptResponse>>> getMyAttempts(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @ParameterObject @Validated @ModelAttribute AttemptSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(quizAttemptService.getMyAttempts(userDetail.getId(), request)));
    }

    @GetMapping("/{id}/attempts")
    @Operation(
            summary = "Lịch sử làm bài của một quiz",
            description = "Trả quiz chi tiết (passages, questions kèm options và đáp án đúng) cùng các lượt làm của "
                    + "user hiện tại, mới nhất trước. Mỗi lượt có questions[] = cặp { question (kèm options), "
                    + "answer } cho mọi câu của quiz, sắp theo position; answer = null là câu chưa trả lời.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND / QUIZ_NO_ATTEMPTS (code 1131): user hiện tại chưa nộp lượt"
                                + " nào cho quiz này")
            })
    public ResponseEntity<ApiResponse<QuizAttemptHistoryResponse>> getQuizAttempts(
            @AuthenticationPrincipal CustomUserDetail userDetail, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quizAttemptService.getQuizAttempts(userDetail.getId(), id)));
    }

    @PutMapping("/{id}/attempts")
    @Operation(
            summary = "Nộp bài (thêm một lượt làm)",
            description = "Chấm điểm và lưu lượt làm cho user hiện tại (lấy từ token, không nhận userId từ body). "
                    + "SINGLE_CHOICE gửi optionId, FILL_BLANK gửi answerText (so không phân biệt hoa thường / khoảng "
                    + "trắng thừa), bỏ trống cả hai = bỏ qua câu. totalQuestions là tổng số câu của quiz. "
                    + "Response có questions[] = cặp { question (kèm options và đáp án đúng), answer } cho mọi câu "
                    + "của quiz, sắp theo position (cùng shape với GET /api/v1/quizzes/{id}/attempts); "
                    + "answer = null là câu không gửi lên. "
                    + "Sau khi quiz có lượt làm thì không sửa được đáp án đúng / loại câu / xoá câu hỏi. "
                    + "Nếu quiz có topic_id: cập nhật lịch ôn FSRS (user_topic_progress) của cặp (skill, topic) "
                    + "của quiz cho user nộp bài. Điểm thang 10 = correctCount × 10 / totalQuestions, làm tròn 1 chữ "
                    + "số thập phân (HALF_UP), đổi sang rating: ≥7.5 → 4, ≥5 → 3, ≥2.5 → 2, còn lại → 1. "
                    + "Tiến độ chỉ được lưu, không trả trong response. Quiz dùng custom_topic hoặc cặp (skill, "
                    + "topic) đã bị gỡ khỏi skill_topics thì không cập nhật tiến độ (vẫn lưu lượt làm). "
                    + "Lỗi khi lưu tiến độ trả 9999 và không lưu lượt làm.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(examples = @ExampleObject(value = SUBMIT_REQUEST_EXAMPLE))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        content = @Content(examples = @ExampleObject(value = ATTEMPT_RESPONSE_EXAMPLE))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = SUBMIT_BAD_REQUEST_DESCRIPTION),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Không phải người tạo quiz"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "QUIZ_NOT_FOUND"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "QUIZ_NOT_READY (GENERATING / FAILED) / QUIZ_HAS_NO_QUESTIONS")
            })
    public ResponseEntity<ApiResponse<AttemptResponse>> submitAttempt(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @PathVariable Long id,
            @Validated @RequestBody AttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(quizAttemptService.submitAttempt(userDetail.getId(), id, request)));
    }
}
