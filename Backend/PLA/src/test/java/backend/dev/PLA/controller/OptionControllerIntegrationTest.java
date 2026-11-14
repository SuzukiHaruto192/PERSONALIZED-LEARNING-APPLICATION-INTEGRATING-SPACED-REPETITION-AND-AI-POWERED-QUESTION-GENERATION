package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // ===================== POST =====================

    @Nested
    class Create {
        @Test
        void threeOptionsWithoutPosition_getPositions1To3_andContentTrimmed() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

            for (int expected = 1; expected <= 3; expected++) {
                mockMvc.perform(json(
                                        post("/api/v1/questions/{questionId}/options", questionId),
                                        body(null, " go ", false))
                                .with(as(userA)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id", notNullValue()))
                        .andExpect(jsonPath("$.data.questionId").value(questionId))
                        .andExpect(jsonPath("$.data.position").value(expected))
                        .andExpect(jsonPath("$.data.content").value("go"))
                        .andExpect(jsonPath("$.data.isCorrect").value(false));
            }
            assertEquals(3, count("options"));
        }

        @Test
        void setOption2Correct_thenOption3Correct_onlyOption3RemainsCorrect() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long o1 = createOption(questionId, body(null, "go", false));
            long o2 = createOption(questionId, body(null, "goes", false));
            long o3 = createOption(questionId, body(null, "went", false));

            mockMvc.perform(json(put("/api/v1/options/{id}", o2), body(null, "goes", true))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCorrect").value(true));
            assertEquals(List.of(o2), correctOptionIds(questionId));

            mockMvc.perform(json(put("/api/v1/options/{id}", o3), body(null, "went", true))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCorrect").value(true))
                    .andExpect(jsonPath("$.data.position").value(3));
            assertEquals(List.of(o3), correctOptionIds(questionId));

            mockMvc.perform(get("/api/v1/questions/{questionId}/options", questionId)
                            .with(as(userA)))
                    .andExpect(jsonPath("$.data[*].id", contains((int) o1, (int) o2, (int) o3)))
                    .andExpect(jsonPath("$.data[*].isCorrect", contains(false, false, true)));
        }

        @Test
        void createCorrectOption_unsetsPreviousCorrect() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long first = createOption(questionId, body(null, "go", true));
            long second = createOption(questionId, body(null, "goes", true));

            assertEquals(List.of(second), correctOptionIds(questionId));
            assertEquals(
                    false, jdbc.queryForObject("SELECT is_correct FROM options WHERE id = ?", Boolean.class, first));
        }

        @Test
        void seventhOption_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            for (int i = 1; i <= 6; i++) {
                insertOption(questionId, i, "O" + i, false);
            }

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "O7", false))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_OPTION_LIMIT.getCode()));
            assertEquals(6, count("options"));
        }

        @Test
        void fillBlankQuestion_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "FILL_BLANK");

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "go", true))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPTION_NOT_ALLOWED.getCode()));
            assertEquals(0, count("options"));
        }

        @Test
        void blankContentAndMissingIsCorrect_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "  ", null))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.content").value("OPTION_CONTENT_BLANK"))
                    .andExpect(jsonPath("$.errors.isCorrect").value("ANSWER_BLANK"));
        }

        @Test
        void positionOutOfRange_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(40_000, "go", false))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.position").value("OPTION_POSITION_INVALID"));
        }

        @Test
        void duplicatedPosition_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            insertOption(questionId, 2, "go", false);

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(2, "goes", false))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPTION_POSITION_DUPLICATED.getCode()));
        }

        @Test
        void quizWithAttempts_returns409() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, "SINGLE_CHOICE");
            insertAttempt(quizId);

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "go", false))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(0, count("options"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "go", false))
                            .with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "GENERATING"), 1, "SINGLE_CHOICE");

            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body(null, "go", false))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
        }

        @Test
        void questionNotExisting_returns404() throws Exception {
            mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", 999_999), body(null, "go", false))
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_NOT_FOUND.getCode()));
        }
    }

    // ===================== isComplete =====================

    @Test
    void isComplete_followsOptionCountAndCorrectAnswer() throws Exception {
        long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");

        long correct = createOption(questionId, body(null, "goes", true));
        expectComplete(questionId, false);

        createOption(questionId, body(null, "go", false));
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

    // ===================== PUT =====================

    @Nested
    class Update {
        @Test
        void withoutPosition_keepsPosition() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 4, "A", false);

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(null, "Edited", false))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.position").value(4))
                    .andExpect(jsonPath("$.data.content").value("Edited"));
        }

        @Test
        void unsetCorrect_returns200() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", true);

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(null, "A", false))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCorrect").value(false));
            assertEquals(List.of(), correctOptionIds(questionId));
        }

        @Test
        void duplicatedPosition_returns400() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", false);
            insertOption(questionId, 2, "B", false);

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(2, "A", false))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPTION_POSITION_DUPLICATED.getCode()));
        }

        @Test
        void quizWithAttempts_canEditContent_butNotIsCorrect() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", false);
            insertAttempt(quizId);

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(3, "Edited", false))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Edited"))
                    .andExpect(jsonPath("$.data.position").value(3));

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(null, "Edited", true))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(List.of(), correctOptionIds(questionId));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long optionId = insertOption(insertQuestion(insertQuiz(userA, "READY"), 1, "SINGLE_CHOICE"), 1, "A", false);

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(null, "Hacked", true))
                            .with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            assertEquals("A", jdbc.queryForObject("SELECT content FROM options WHERE id = ?", String.class, optionId));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(json(put("/api/v1/options/{id}", 999_999), body(null, "A", false))
                            .with(as(userA)))
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
        void quizWithAttempts_returns409_andKeepsData_butContentStillEditable() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, "SINGLE_CHOICE");
            long optionId = insertOption(questionId, 1, "A", true);
            insertAttempt(quizId);

            mockMvc.perform(delete("/api/v1/options/{id}", optionId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));
            assertEquals(1, count("options"));

            mockMvc.perform(json(put("/api/v1/options/{id}", optionId), body(null, "A edited", true))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("A edited"));
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
    void swagger_documentsAllFiveEndpoints() throws Exception {
        String byQuestion = "$.paths['/api/v1/questions/{questionId}/options']";
        String byId = "$.paths['/api/v1/options/{id}']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(byQuestion + ".post", notNullValue()))
                .andExpect(jsonPath(byQuestion + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".put", notNullValue()))
                .andExpect(jsonPath(byId + ".delete", notNullValue()))
                .andExpect(jsonPath(
                        byQuestion + ".post.responses['400'].content['*/*'].examples['Quá 6 đáp án']", notNullValue()));
    }

    // ===================== Helpers =====================

    Map<String, Object> body(Integer position, String content, Boolean isCorrect) {
        Map<String, Object> body = new HashMap<>();
        body.put("position", position);
        body.put("content", content);
        body.put("isCorrect", isCorrect);
        return body;
    }

    long createOption(long questionId, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(json(post("/api/v1/questions/{questionId}/options", questionId), body)
                        .with(as(userA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    void expectComplete(long questionId, boolean complete) throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", questionId).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isComplete").value(complete));
    }

    List<Long> correctOptionIds(long questionId) {
        return jdbc.queryForList(
                "SELECT id FROM options WHERE question_id = ? AND is_correct ORDER BY id", Long.class, questionId);
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
