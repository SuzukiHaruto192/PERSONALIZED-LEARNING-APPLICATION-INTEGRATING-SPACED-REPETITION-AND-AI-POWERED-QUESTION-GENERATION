package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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

/** Integration test cho 5 endpoint passage trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PassageControllerIntegrationTest {

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
        // Giữ skills / quiz_formats do V2 seed; xoá dữ liệu còn lại (CASCADE kéo theo quizzes, passages, ...)
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
    }

    // ===================== GET list =====================

    @Nested
    class GetList {
        @Test
        void returnsPassagesOrderedByPosition_withQuestionCount() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long second = insertPassage(quizId, 2);
            long first = insertPassage(quizId, 1);
            insertQuestion(quizId, 1, second);
            insertQuestion(quizId, 2, second);

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/passages", quizId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[*].id", contains((int) first, (int) second)))
                    .andExpect(jsonPath("$.data[0].questionCount").value(0))
                    .andExpect(jsonPath("$.data[1].questionCount").value(2));
        }

        @Test
        void otherUsersQuiz_returns403() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            insertPassage(quizId, 1);

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/passages", quizId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void quizNotExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{quizId}/passages", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }
    }

    // ===================== GET detail =====================

    @Nested
    class Detail {
        @Test
        void ownPassage_returns200() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            insertQuestion(quizId, 1, passageId);

            mockMvc.perform(get("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(passageId))
                    .andExpect(jsonPath("$.data.quizId").value(quizId))
                    .andExpect(jsonPath("$.data.position").value(1))
                    .andExpect(jsonPath("$.data.questionCount").value(1));
        }

        @Test
        void passageOfAnotherUsersQuiz_returns403() throws Exception {
            long passageId = insertPassage(insertQuiz(userA, "READY"), 1);

            mockMvc.perform(get("/api/v1/passages/{id}", passageId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/passages/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_NOT_FOUND.getCode()));
        }
    }

    // ===================== DELETE =====================

    @Nested
    class Delete {
        @Test
        void owner_returns204() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            insertPassage(quizId, 2);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertEquals(1, count("passages"));
            assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM passages WHERE id = ?", Long.class, passageId));
        }

        @Test
        void passageWithQuestions_returns409_andKeepsData() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            insertQuestion(quizId, 1, passageId);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_HAS_QUESTIONS.getCode()));

            assertEquals(1, count("passages"));
            assertEquals(1, count("questions"));
            assertEquals(
                    passageId,
                    jdbc.queryForObject("SELECT passage_id FROM questions WHERE quiz_id = ?", Long.class, quizId));
        }

        @Test
        void passageReferencedByFlashcard_returns204_andDetachesFlashcard() throws Exception {
            long passageId = insertPassage(insertQuiz(userA, "READY"), 1);
            long flashcardId = jdbc.queryForObject(
                    "INSERT INTO flashcard (front_content, back_content, source_passage_id) "
                            + "VALUES ('get up', 'thức dậy', ?) RETURNING id",
                    Long.class,
                    passageId);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertEquals(0, count("passages"));
            assertEquals(1, count("flashcard"));
            Map<String, Object> flashcard = jdbc.queryForMap("SELECT * FROM flashcard WHERE id = ?", flashcardId);
            assertNull(flashcard.get("source_passage_id"));
        }

        @Test
        void quizWithAttempts_returns409_andKeepsData() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            insertAttempt(quizId);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(1, count("passages"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long passageId = insertPassage(insertQuiz(userA, "READY"), 1);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userB)))
                    .andExpect(status().isForbidden());
            assertEquals(1, count("passages"));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long passageId = insertPassage(insertQuiz(userA, "GENERATING"), 1);

            mockMvc.perform(delete("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
            assertEquals(1, count("passages"));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/passages/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_NOT_FOUND.getCode()));
        }
    }

    // ===================== GET /quizzes/{id} kèm passages =====================

    @Test
    void quizDetail_includesPassagesOrderedByPosition() throws Exception {
        long quizId = insertQuiz(userA, "READY");
        long third = insertPassage(quizId, 3);
        long first = insertPassage(quizId, 1);
        long second = insertPassage(quizId, 2);
        insertQuestion(quizId, 1, first);

        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages", hasSize(3)))
                .andExpect(jsonPath("$.data.passages[*].id", contains((int) first, (int) second, (int) third)))
                .andExpect(jsonPath("$.data.passages[*].position", contains(1, 2, 3)))
                .andExpect(jsonPath("$.data.passages[0].questionCount").value(1));
    }

    @Test
    void quizDetail_withoutPassages_returnsEmptyArray() throws Exception {
        long quizId = insertQuiz(userA, "READY");

        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passages", hasSize(0)));
    }

    @Test
    void quizList_doesNotIncludePassages() throws Exception {
        insertPassage(insertQuiz(userA, "READY"), 1);

        mockMvc.perform(get("/api/v1/quizzes").with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].passages").doesNotExist());
    }

    // ===================== Swagger =====================

    @Test
    void swagger_documentsReadAndDeleteEndpointsOnly() throws Exception {
        // Tạo / sửa do AI đảm nhiệm: không còn POST / PUT
        String byQuiz = "$.paths['/api/v1/quizzes/{quizId}/passages']";
        String byId = "$.paths['/api/v1/passages/{id}']";
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

    long insertPassage(long quizId, int position) {
        return jdbc.queryForObject(
                "INSERT INTO passages (quiz_id, position, content) VALUES (?, ?, 'Original') RETURNING id",
                Long.class,
                quizId,
                position);
    }

    long insertQuestion(long quizId, int position, long passageId) {
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, passage_id, position, type, content) "
                        + "VALUES (?, ?, ?, 'SINGLE_CHOICE', 'Question?') RETURNING id",
                Long.class,
                quizId,
                passageId,
                position);
    }

    void insertAttempt(long quizId) {
        jdbc.update("INSERT INTO quiz_attempts (quiz_id, user_id) VALUES (?, ?)", quizId, userB);
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
