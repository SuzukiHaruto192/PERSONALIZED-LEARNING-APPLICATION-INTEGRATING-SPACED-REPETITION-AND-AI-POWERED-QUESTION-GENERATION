package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integration test cho 3 endpoint lịch sử làm bài trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QuizAttemptControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    final ObjectMapper objectMapper = new ObjectMapper();

    long userA;
    long userB;
    long readingId;

    // Quiz mẫu của userA: q1 SINGLE_CHOICE (o1 sai, o2 đúng), q2 FILL_BLANK (correct_text "goes"), q3 SINGLE_CHOICE
    long quizId;
    long q1;
    long q2;
    long q3;
    long o1;
    long o2;
    long o3Correct;

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);

        quizId = insertQuiz(userA, "READY", "Quiz A");
        q1 = insertQuestion(quizId, 1, "SINGLE_CHOICE");
        q2 = insertQuestion(quizId, 2, "FILL_BLANK");
        q3 = insertQuestion(quizId, 3, "SINGLE_CHOICE");
        o1 = insertOption(q1, 1, false);
        o2 = insertOption(q1, 2, true);
        o3Correct = insertOption(q3, 1, true);
    }

    // ===================== PUT /quizzes/{id}/attempts =====================

    @Nested
    class Submit {
        @Test
        void gradesAndSavesAttempt() throws Exception {
            mockMvc.perform(json(
                                    put("/api/v1/quizzes/{id}/attempts", quizId),
                                    body(answer(q2, null, "  GOES "), answer(q1, o2, null))) // q3 bỏ trống
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.quiz.id").value(quizId))
                    .andExpect(jsonPath("$.data.quiz.name").value("Quiz A"))
                    .andExpect(jsonPath("$.data.correctCount").value(2))
                    .andExpect(jsonPath("$.data.totalQuestions").value(3))
                    .andExpect(jsonPath("$.data.submittedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.answers").doesNotExist())
                    // Mọi câu của quiz theo position, kèm options + đáp án đúng; q3 không gửi lên → answer = null
                    .andExpect(jsonPath("$.data.questions[*].question.id", contains((int) q1, (int) q2, (int) q3)))
                    .andExpect(jsonPath("$.data.questions[0].question.options[*].id", contains((int) o1, (int) o2)))
                    .andExpect(jsonPath("$.data.questions[0].question.options[1].isCorrect")
                            .value(true))
                    .andExpect(
                            jsonPath("$.data.questions[1].question.correctText").value("goes"))
                    .andExpect(jsonPath("$.data.questions[0].answer.selectedOptionId")
                            .value(o2))
                    .andExpect(jsonPath("$.data.questions[0].answer.questionId").value(q1))
                    .andExpect(jsonPath("$.data.questions[0].answer.attemptId").isNumber())
                    .andExpect(jsonPath("$.data.questions[1].answer.answerText").value("GOES"))
                    .andExpect(jsonPath("$.data.questions[0].answer.isCorrect").value(true))
                    .andExpect(jsonPath("$.data.questions[1].answer.isCorrect").value(true))
                    .andExpect(jsonPath("$.data.questions[2].answer").value(nullValue()));

            assertEquals(1, count("quiz_attempts"));
            assertEquals(2, count("attempt_answers"));
            assertEquals(userA, jdbc.queryForObject("SELECT user_id FROM quiz_attempts", Long.class));
        }

        @Test
        void wrongAnswersAndSkippedQuestion_countedAsIncorrect() throws Exception {
            mockMvc.perform(json(
                                    put("/api/v1/quizzes/{id}/attempts", quizId),
                                    body(answer(q1, o1, null), answer(q2, null, "go"), answer(q3, null, null)))
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.correctCount").value(0))
                    .andExpect(jsonPath("$.data.questions", hasSize(3)))
                    .andExpect(jsonPath("$.data.questions[*].answer.isCorrect", contains(false, false, false)));
        }

        @Test
        void userIdInBody_isIgnored() throws Exception {
            Map<String, Object> body = body(answer(q1, o2, null));
            body.put("userId", userB);
            body.put("quizId", 999);

            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isCreated());
            assertEquals(userA, jdbc.queryForObject("SELECT user_id FROM quiz_attempts", Long.class));
        }

        @Test
        void quizNotFound_returns404() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", 9999), body(answer(q1, o2, null)))
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }

        @Test
        void notOwner_returns403() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body(answer(q1, o2, null)))
                            .with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            assertEquals(0, count("quiz_attempts"));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long generating = insertQuiz(userA, "GENERATING", "Gen");
            long q = insertQuestion(generating, 1, "FILL_BLANK");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", generating), body(answer(q, null, "goes")))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_READY.getCode()));
        }

        @Test
        void failedQuiz_returns409() throws Exception {
            long failed = insertQuiz(userA, "FAILED", "Failed");
            long q = insertQuestion(failed, 1, "FILL_BLANK");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", failed), body(answer(q, null, "goes")))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_READY.getCode()));
            assertEquals(0, count("quiz_attempts"));
        }

        @Test
        void systemQuiz_returns403() throws Exception {
            // Quiz hệ thống (created_by = null) cũng không làm được, giống quy tắc xem quiz
            long system = jdbc.queryForObject(
                    "INSERT INTO quizzes (skill_id, custom_topic, difficulty, title, status) "
                            + "VALUES (?, 'custom', 2, 'System', 'READY') RETURNING id",
                    Long.class,
                    readingId);
            long q = insertQuestion(system, 1, "FILL_BLANK");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", system), body(answer(q, null, "goes")))
                            .with(as(userA)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body(answer(q1, o2, null))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
            assertEquals(0, count("quiz_attempts"));
        }

        @Test
        void answersSortedByPosition_andFillBlankNormalizesInnerSpaces() throws Exception {
            jdbc.update("UPDATE questions SET correct_text = 'goes on' WHERE id = ?", q2);

            mockMvc.perform(json(
                                    put("/api/v1/quizzes/{id}/attempts", quizId),
                                    body(
                                            answer(q3, o3Correct, null),
                                            answer(q2, null, " Goes    ON "),
                                            answer(q1, o1, null)))
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(
                            jsonPath("$.data.questions[*].answer.questionId", contains((int) q1, (int) q2, (int) q3)))
                    .andExpect(jsonPath("$.data.questions[*].answer.isCorrect", contains(false, true, true)))
                    .andExpect(jsonPath("$.data.questions[1].answer.answerText").value("Goes    ON"))
                    .andExpect(jsonPath("$.data.correctCount").value(2));

            assertEquals(
                    "Goes    ON",
                    jdbc.queryForObject(
                            "SELECT answer_text FROM attempt_answers WHERE question_id = ?", String.class, q2));
        }

        @Test
        void savesAnswerColumns() throws Exception {
            mockMvc.perform(json(
                                    put("/api/v1/quizzes/{id}/attempts", quizId),
                                    body(answer(q1, o2, null), answer(q3, null, null)))
                            .with(as(userA)))
                    .andExpect(status().isCreated());

            Map<String, Object> single = jdbc.queryForMap(
                    "SELECT selected_option_id, answer_text, is_correct FROM attempt_answers WHERE question_id = ?",
                    q1);
            assertEquals(o2, ((Number) single.get("selected_option_id")).longValue());
            assertEquals(null, single.get("answer_text"));
            assertEquals(true, single.get("is_correct"));

            Map<String, Object> skipped = jdbc.queryForMap(
                    "SELECT selected_option_id, answer_text, is_correct FROM attempt_answers WHERE question_id = ?",
                    q3);
            assertEquals(null, skipped.get("selected_option_id"));
            assertEquals(null, skipped.get("answer_text"));
            assertEquals(false, skipped.get("is_correct"));

            Map<String, Object> attempt = jdbc.queryForMap(
                    "SELECT correct_count, total_questions, started_at, submitted_at FROM quiz_attempts");
            assertEquals(1, attempt.get("correct_count"));
            assertEquals(3, attempt.get("total_questions"));
            assertEquals(attempt.get("started_at"), attempt.get("submitted_at"));
        }

        @Test
        void answerTextTooLong_returns400() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body(answer(q2, null, "a".repeat(256))))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors['answers[0].answerText']").value("ATTEMPT_ANSWER_TEXT_SIZE"));
            assertEquals(0, count("quiz_attempts"));
        }

        @Test
        void missingAnswersField_returns400() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), new HashMap<>())
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.answers").value("ATTEMPT_ANSWERS_EMPTY"));
        }

        @Test
        void afterAttempt_correctAnswerCannotBeChanged() throws Exception {
            submit(quizId, body(answer(q1, o2, null)));

            // Đã có lượt làm thì không xoá được câu hỏi (đáp án đúng bị khoá)
            mockMvc.perform(delete("/api/v1/questions/{id}", q3).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
        }

        @Test
        void quizWithoutQuestions_returns409() throws Exception {
            long empty = insertQuiz(userA, "READY", "Empty");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", empty), body(answer(q1, o2, null)))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_NO_QUESTIONS.getCode()));
        }

        @Test
        void emptyAnswers_returns400() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body())
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.answers").value("ATTEMPT_ANSWERS_EMPTY"));
        }

        @Test
        void missingQuestionId_returns400() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body(answer(null, o2, null)))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors['answers[0].questionId']").value("ATTEMPT_QUESTION_ID_BLANK"));
        }

        @Test
        void questionOfOtherQuiz_returns400() throws Exception {
            long otherQuestion = insertQuestion(insertQuiz(userA, "READY", "Other"), 1, "FILL_BLANK");

            expectBusinessError(body(answer(otherQuestion, null, "goes")), ErrorCode.ATTEMPT_QUESTION_NOT_IN_QUIZ);
        }

        @Test
        void duplicatedQuestion_returns400() throws Exception {
            expectBusinessError(
                    body(answer(q1, o2, null), answer(q1, o1, null)), ErrorCode.ATTEMPT_QUESTION_DUPLICATED);
        }

        @Test
        void optionOfOtherQuestion_returns400() throws Exception {
            expectBusinessError(body(answer(q1, o3Correct, null)), ErrorCode.ATTEMPT_OPTION_NOT_IN_QUESTION);
        }

        @Test
        void answerTypeMismatch_returns400() throws Exception {
            expectBusinessError(body(answer(q1, null, "goes")), ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH);
            expectBusinessError(body(answer(q2, o2, null)), ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH);
        }

        void expectBusinessError(Map<String, Object> body, ErrorCode errorCode) throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}/attempts", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()));
            assertEquals(0, count("quiz_attempts"));
        }
    }

    // ===================== GET /quizzes/attempts =====================

    @Nested
    class ListAll {
        @Test
        void returnsOnlyMyAttempts_newestFirst_withoutAnswers() throws Exception {
            long quizB = insertQuiz(userA, "READY", "Quiz B");
            long qb = insertQuestion(quizB, 1, "FILL_BLANK");
            long first = submit(quizId, body(answer(q1, o2, null)));
            long second = submit(quizB, body(answer(qb, null, "goes")));
            insertAttemptOf(userB, quizId);

            mockMvc.perform(get("/api/v1/quizzes/attempts").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[*].id", contains((int) second, (int) first)))
                    .andExpect(jsonPath("$.data.content[0].quiz.name").value("Quiz B"))
                    .andExpect(jsonPath("$.data.content[0].skill.id").value(readingId))
                    .andExpect(jsonPath("$.data.content[0].answers").doesNotExist());
        }

        @Test
        void paging() throws Exception {
            for (int i = 0; i < 3; i++) {
                submit(quizId, body(answer(q1, o2, null)));
            }

            mockMvc.perform(get("/api/v1/quizzes/attempts?page=1&size=2").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/attempts"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        void noAttempts_returnsEmptyPage() throws Exception {
            insertAttemptOf(userB, quizId);

            mockMvc.perform(get("/api/v1/quizzes/attempts").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        void sizeAbove100_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/attempts?size=101").with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"));
        }

        @Test
        void invalidPaging_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/attempts?page=-1&size=0").with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.page").value("PAGE_INVALID"))
                    .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"));
        }
    }

    // ===================== GET /quizzes/{id}/attempts =====================

    @Nested
    class ByQuiz {
        @Test
        void returnsQuizDetailAndMyAttemptsWithAnswers() throws Exception {
            long first = submit(quizId, body(answer(q1, o1, null)));
            long second = submit(quizId, body(answer(q1, o2, null), answer(q2, null, "goes")));
            insertAttemptOf(userB, quizId);

            mockMvc.perform(get("/api/v1/quizzes/{id}/attempts", quizId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.quiz.id").value(quizId))
                    .andExpect(jsonPath("$.data.quiz.questions", hasSize(3)))
                    .andExpect(jsonPath("$.data.quiz.questions[0].options", hasSize(2)))
                    .andExpect(jsonPath("$.data.attempts[*].id", contains((int) second, (int) first)))
                    .andExpect(jsonPath("$.data.attempts[0].correctCount").value(2))
                    .andExpect(jsonPath("$.data.attempts[0].answers").doesNotExist())
                    // Mọi câu của quiz, sắp theo position, mỗi câu kèm options và câu trả lời (null = chưa trả lời)
                    .andExpect(jsonPath("$.data.attempts[0].questions", hasSize(3)))
                    .andExpect(jsonPath(
                            "$.data.attempts[0].questions[*].question.id", contains((int) q1, (int) q2, (int) q3)))
                    .andExpect(jsonPath("$.data.attempts[0].questions[0].question.options", hasSize(2)))
                    .andExpect(jsonPath("$.data.attempts[0].questions[0].answer.selectedOptionId")
                            .value(o2))
                    .andExpect(jsonPath("$.data.attempts[0].questions[1].answer.answerText")
                            .value("goes"))
                    .andExpect(
                            jsonPath("$.data.attempts[0].questions[2].answer").value(nullValue()))
                    .andExpect(jsonPath("$.data.attempts[1].questions[0].answer.selectedOptionId")
                            .value(o1))
                    .andExpect(jsonPath("$.data.attempts[1].questions[0].answer.isCorrect")
                            .value(false))
                    .andExpect(
                            jsonPath("$.data.attempts[1].questions[1].answer").value(nullValue()));
        }

        @Test
        void noAttempts_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{id}/attempts", quizId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.attempts", hasSize(0)));
        }

        @Test
        void quizNotFound_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{id}/attempts", 9999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{id}/attempts", quizId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        void notOwner_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{id}/attempts", quizId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }
    }

    // ===================== Helpers =====================

    long submit(long quiz, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(
                        json(put("/api/v1/quizzes/{id}/attempts", quiz), body).with(as(userA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    @SafeVarargs
    final Map<String, Object> body(Map<String, Object>... answers) {
        Map<String, Object> body = new HashMap<>();
        body.put("answers", new ArrayList<>(List.of(answers)));
        return body;
    }

    Map<String, Object> answer(Long questionId, Long optionId, String answerText) {
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", questionId);
        answer.put("optionId", optionId);
        answer.put("answerText", answerText);
        return answer;
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    RequestPostProcessor as(long userId) {
        return user(new CustomUserDetail(User.builder()
                .id(userId)
                .username("user" + userId)
                .password("x")
                .build()));
    }

    long insertUser(String username) {
        return jdbc.queryForObject(
                "INSERT INTO users (username, password) VALUES (?, 'x') RETURNING id", Long.class, username);
    }

    long insertQuiz(long createdBy, String status, String title) {
        return jdbc.queryForObject(
                "INSERT INTO quizzes (created_by, skill_id, custom_topic, difficulty, title, status) "
                        + "VALUES (?, ?, 'custom', 2, ?, ?) RETURNING id",
                Long.class,
                createdBy,
                readingId,
                title,
                status);
    }

    long insertQuestion(long quiz, int position, String type) {
        String correctText = "FILL_BLANK".equals(type) ? "goes" : null;
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, position, type, content, correct_text) "
                        + "VALUES (?, ?, ?, 'Question?', ?) RETURNING id",
                Long.class,
                quiz,
                position,
                type,
                correctText);
    }

    long insertOption(long questionId, int position, boolean isCorrect) {
        return jdbc.queryForObject(
                "INSERT INTO options (question_id, position, content, is_correct) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                questionId,
                position,
                "O" + position,
                isCorrect);
    }

    void insertAttemptOf(long userId, long quiz) {
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, submitted_at, correct_count, total_questions) "
                        + "VALUES (?, ?, now(), 0, 3)",
                quiz,
                userId);
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
