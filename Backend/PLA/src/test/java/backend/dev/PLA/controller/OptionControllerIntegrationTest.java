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
import java.util.List;
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

/** Integration test cho 5 endpoint option trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OptionControllerIntegrationTest {

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
        // Giữ skills / quiz_formats do V2 seed; xoá dữ liệu còn lại (CASCADE kéo theo quizzes, options, ...)
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
    }

    // ===================== isComplete =====================

    @Test
    void isComplete_followsOptionCountAndCorrectAnswer() throws Exception {
        long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

        long correct = insertOption(questionId, 1, "goes", true);
        expectComplete(questionId, false);

        insertOption(questionId, 2, "go", false);
        expectComplete(questionId, true);

        mockMvc.perform(delete("/api/v1/options/{id}", correct).with(as(userA))).andExpect(status().isNoContent());
        expectComplete(questionId, false);
    }

    // ===================== GET list / detail =====================

    @Nested
    class Read {
        @Test
        void list_orderedByPosition() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long second = insertOption(questionId, 2, "B", false);
            long first = insertOption(questionId, 1, "A", true);

            mockMvc.perform(get("/api/v1/questions/{questionId}/options", questionId)
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[*].id", contains((int) first, (int) second)));
        }

        @Test
        void list_otherUser_returns403() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

            mockMvc.perform(get("/api/v1/questions/{questionId}/options", questionId)
                            .with(as(userB)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void list_questionNotExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/questions/{questionId}/options", 999_999)
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_NOT_FOUND.getCode()));
        }

        @Test
        void detail_returns200() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", true);

            mockMvc.perform(get("/api/v1/options/{id}", optionId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(optionId))
                    .andExpect(jsonPath("$.data.questionId").value(questionId))
                    .andExpect(jsonPath("$.data.content").value("A"))
                    .andExpect(jsonPath("$.data.isCorrect").value(true));
        }

        @Test
        void detail_otherUser_returns403() throws Exception {
            long optionId = insertOption(insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE"), 1, "A", true);

            mockMvc.perform(get("/api/v1/options/{id}", optionId).with(as(userB)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void detail_notExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/options/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPTION_NOT_FOUND.getCode()));
        }
    }

    // ===================== DELETE =====================

    @Nested
    class Delete {
        @Test
        void owner_returns204_withoutRenumbering() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", false);
            insertOption(questionId, 2, "B", false);

            mockMvc.perform(delete("/api/v1/options/{id}", optionId).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertEquals(1, count("options"));
            assertEquals(
                    2,
                    jdbc.queryForObject(
                            "SELECT position FROM options WHERE question_id = ?", Integer.class, questionId));
        }

        @Test
        void quizWithAttempts_returns409_andKeepsData() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", true);
            insertAttempt(quizId);

            mockMvc.perform(delete("/api/v1/options/{id}", optionId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(1, count("options"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long optionId = insertOption(insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE"), 1, "A", false);

            mockMvc.perform(delete("/api/v1/options/{id}", optionId).with(as(userB)))
                    .andExpect(status().isForbidden());
            assertEquals(1, count("options"));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/options/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPTION_NOT_FOUND.getCode()));
        }
    }

    // ===================== Swagger =====================

    @Test
    void swagger_documentsReadAndDeleteEndpointsOnly() throws Exception {
        // Tạo / sửa do AI đảm nhiệm: không còn POST / PUT
        String byQuestion = "$.paths['/api/v1/questions/{questionId}/options']";
        String byId = "$.paths['/api/v1/options/{id}']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(byQuestion + ".get", notNullValue()))
                .andExpect(jsonPath(byQuestion + ".post").doesNotExist())
                .andExpect(jsonPath(byId + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".delete", notNullValue()))
                .andExpect(jsonPath(byId + ".put").doesNotExist());
    }

    // ===================== Helpers =====================

    void expectComplete(long questionId, boolean complete) throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", questionId).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isComplete").value(complete));
    }

    List<Long> correctOptionIds(long questionId) {
        return jdbc.queryForList(
                "SELECT id FROM options WHERE question_id = ? AND is_correct ORDER BY id", Long.class, questionId);
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

    long insertQuiz(long createdBy, String status) {
        return jdbc.queryForObject(
                "INSERT INTO quizzes (created_by, skill_id, custom_topic, difficulty, title, status) "
                        + "VALUES (?, ?, 'custom', 2, 'Quiz', ?) RETURNING id",
                Long.class,
                createdBy,
                readingId,
                status);
    }

    long insertQuestion(long quizId, int position, String type) {
        String correctText = "FILL_BLANK".equals(type) ? "goes" : null;
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, position, type, content, correct_text) "
                        + "VALUES (?, ?, ?, 'Question?', ?) RETURNING id",
                Long.class,
                quizId,
                position,
                type,
                correctText);
    }

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
