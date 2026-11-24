package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integration test cho 5 endpoint question trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QuestionControllerIntegrationTest {

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

    @BeforeEach
    void resetData() {
        // Giữ skills / quiz_formats do V2 seed; xoá dữ liệu còn lại (CASCADE kéo theo quizzes, questions, ...)
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
    }

    // ===================== GET list =====================

    @Nested
    class GetList {
        @Test
        void returnsQuestionsOrderedByPosition_withOptionsOrdered() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long second = insertQuestion(quizId, 2, null);
            long first = insertQuestion(quizId, 1, null);
            insertOption(first, 2, "B", false);
            insertOption(first, 1, "A", true);

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", quizId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[*].id", contains((int) first, (int) second)))
                    .andExpect(jsonPath("$.data[0].options[*].content", contains("A", "B")))
                    .andExpect(jsonPath("$.data[0].options[0].isCorrect").value(true))
                    .andExpect(jsonPath("$.data[0].options[0].questionId").value(first))
                    .andExpect(jsonPath("$.data[0].isComplete").value(true))
                    .andExpect(jsonPath("$.data[1].options", hasSize(0)));
        }

        @Test
        void passageOfAnotherQuiz_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long otherPassage = insertPassage(insertQuiz(userA, "READY"), 1);

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", quizId)
                            .param("passageId", String.valueOf(otherPassage))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_NOT_IN_QUIZ.getCode()));
        }

        @Test
        void otherUsersQuiz_returns403() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", quizId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void quizNotExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }
    }

    // ===================== GET detail =====================

    @Nested
    class Detail {
        @Test
        void ownQuestion_returns200WithOptions() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            long questionId = insertQuestion(quizId, 1, passageId);
            insertOption(questionId, 1, "6 a.m.", true);

            mockMvc.perform(get("/api/v1/questions/{id}", questionId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(questionId))
                    .andExpect(jsonPath("$.data.quizId").value(quizId))
                    .andExpect(jsonPath("$.data.passageId").value(passageId))
                    .andExpect(jsonPath("$.data.options", hasSize(1)))
                    .andExpect(jsonPath("$.data.isComplete").value(false));
        }

        @Test
        void questionOfAnotherUsersQuiz_returns403() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, null);

            mockMvc.perform(get("/api/v1/questions/{id}", questionId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/questions/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_NOT_FOUND.getCode()));
        }
    }

    // ===================== DELETE =====================

    @Nested
    class Delete {
        @Test
        void owner_returns204_andDeletesOptions_withoutRenumbering() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            insertQuestion(quizId, 2, null);
            insertOption(questionId, 1, "A", true);
            insertOption(questionId, 2, "B", false);

            mockMvc.perform(delete("/api/v1/questions/{id}", questionId).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertEquals(1, count("questions"));
            assertEquals(0, count("options"));
            assertEquals(
                    2, jdbc.queryForObject("SELECT position FROM questions WHERE quiz_id = ?", Integer.class, quizId));
        }

        @Test
        void quizWithAttempts_returns409_andKeepsData() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            insertOption(questionId, 1, "A", true);
            insertAttempt(quizId);

            mockMvc.perform(delete("/api/v1/questions/{id}", questionId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(1, count("questions"));
            assertEquals(1, count("options"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, null);

            mockMvc.perform(delete("/api/v1/questions/{id}", questionId).with(as(userB)))
                    .andExpect(status().isForbidden());
            assertEquals(1, count("questions"));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/questions/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_NOT_FOUND.getCode()));
        }
    }

    // ===================== GET /quizzes/{id} kèm questions =====================

    @Test
    void quizDetail_includesQuestionsAndOptionsOrderedByPosition() throws Exception {
        long quizId = insertQuiz(userA, "READY");
        long second = insertQuestion(quizId, 2, null);
        long first = insertQuestion(quizId, 1, null);
        insertOption(first, 2, "B", false);
        insertOption(first, 1, "A", true);

        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(2))
                .andExpect(jsonPath("$.data.questions", hasSize(2)))
                .andExpect(jsonPath("$.data.questions[*].id", contains((int) first, (int) second)))
                .andExpect(jsonPath("$.data.questions[0].options[*].content", contains("A", "B")))
                .andExpect(jsonPath("$.data.questions[0].isComplete").value(true));
    }

    @Test
    void quizList_doesNotIncludeQuestions() throws Exception {
        insertQuestion(insertQuiz(userA, "READY"), 1, null);

        mockMvc.perform(get("/api/v1/quizzes").with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].questions").doesNotExist());
    }

    // ===================== Swagger =====================

    @Test
    void swagger_documentsReadAndDeleteEndpointsOnly() throws Exception {
        // Tạo / sửa do AI đảm nhiệm: không còn POST / PUT
        String byQuiz = "$.paths['/api/v1/quizzes/{quizId}/questions']";
        String byId = "$.paths['/api/v1/questions/{id}']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(byQuiz + ".get", notNullValue()))
                .andExpect(jsonPath(byQuiz + ".post").doesNotExist())
                .andExpect(jsonPath(byId + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".delete", notNullValue()))
                .andExpect(jsonPath(byId + ".put").doesNotExist());
    }

    // ===================== Helpers =====================

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

    long insertQuiz(long createdBy, String status) {
        return jdbc.queryForObject(
                "INSERT INTO quizzes (created_by, skill_id, custom_topic, difficulty, title, status) "
                        + "VALUES (?, ?, 'custom', 2, 'Quiz', ?) RETURNING id",
                Long.class,
                createdBy,
                readingId,
                status);
    }

    long insertTopic(String groupType, String slug, String nameVi) {
        long groupId = jdbc.queryForObject(
                "INSERT INTO topic_groups (type, name) VALUES (?, ?) RETURNING id", Long.class, groupType, slug);
        return jdbc.queryForObject(
                "INSERT INTO topics (group_id, slug, name_vi) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                groupId,
                slug,
                nameVi);
    }

    long insertPassage(long quizId, int position) {
        return jdbc.queryForObject(
                "INSERT INTO passages (quiz_id, position, content) VALUES (?, ?, 'Original') RETURNING id",
                Long.class,
                quizId,
                position);
    }

    long insertQuestion(long quizId, int position, Long passageId) {
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, passage_id, position, type, content) "
                        + "VALUES (?, ?, ?, 'SINGLE_CHOICE', 'Question?') RETURNING id",
                Long.class,
                quizId,
                passageId,
                position);
    }

    long insertFillBlank(long quizId, int position, String correctText) {
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, position, type, content, correct_text) "
                        + "VALUES (?, ?, 'FILL_BLANK', 'She ___ to school.', ?) RETURNING id",
                Long.class,
                quizId,
                position,
                correctText);
    }

    // Chèn trực tiếp vì test này không kiểm tra API options
    long insertOption(long questionId, int position, String content, boolean isCorrect) {
        return jdbc.queryForObject(
                "INSERT INTO options (question_id, position, content, is_correct) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                questionId,
                position,
                content,
                isCorrect);
    }

    void insertAttempt(long quizId) {
        jdbc.update("INSERT INTO quiz_attempts (quiz_id, user_id) VALUES (?, ?)", quizId, userB);
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
