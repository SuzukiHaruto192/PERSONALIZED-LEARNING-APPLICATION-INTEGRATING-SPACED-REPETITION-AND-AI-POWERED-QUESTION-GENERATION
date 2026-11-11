package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Integration test cho 5 endpoint quiz trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QuizControllerIntegrationTest {

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
    long listeningId;
    long readingFormatId;
    long listeningFormatId;
    long readingTopicId; // gắn với READING trong skill_topics
    long listeningOnlyTopicId; // chỉ gắn với LISTENING

    @BeforeEach
    void resetData() {
        // Giữ skills / quiz_formats do V2 seed; xoá dữ liệu còn lại (CASCADE kéo theo quizzes, questions, ...)
        jdbc.execute("TRUNCATE users, topic_groups RESTART IDENTITY CASCADE");

        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
        listeningId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'LISTENING'", Long.class);
        readingFormatId = jdbc.queryForObject(
                "SELECT id FROM quiz_formats WHERE skill_id = ? AND code = 'EMAIL'", Long.class, readingId);
        listeningFormatId = jdbc.queryForObject(
                "SELECT id FROM quiz_formats WHERE skill_id = ? AND code = 'PODCAST'", Long.class, listeningId);

        long groupId = jdbc.queryForObject(
                "INSERT INTO topic_groups (type, name) VALUES ('SITUATION', 'Đời sống') RETURNING id", Long.class);
        readingTopicId = insertTopic(groupId, "daily-routine", "Sinh hoạt hằng ngày", readingId);
        listeningOnlyTopicId = insertTopic(groupId, "airport", "Sân bay", listeningId);
    }

    // ===================== POST =====================

    @Nested
    class Create {
        @Test
        void validQuiz_returns201_readyAndOwnedByCurrentUser() throws Exception {
            Map<String, Object> body = validBody();
            // field server tự gán: client gửi thì bị bỏ qua
            body.put("status", "GENERATING");
            body.put("createdBy", userB);
            body.put("description", "   ");

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id", notNullValue()))
                    .andExpect(jsonPath("$.data.status").value("READY"))
                    .andExpect(jsonPath("$.data.createdBy").value(userA))
                    .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.data.title").value("Present Simple - Daily routines"))
                    .andExpect(jsonPath("$.data.description").doesNotExist())
                    .andExpect(jsonPath("$.data.skill.id").value(readingId))
                    .andExpect(jsonPath("$.data.skill.name").value("Reading"))
                    .andExpect(jsonPath("$.data.topic.id").value(readingTopicId))
                    .andExpect(jsonPath("$.data.topic.name").value("Sinh hoạt hằng ngày"))
                    .andExpect(jsonPath("$.data.format.id").value(readingFormatId))
                    .andExpect(jsonPath("$.data.questionCount").value(0));

            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM quizzes");
            assertEquals("READY", row.get("status"));
            assertEquals(userA, ((Number) row.get("created_by")).longValue());
            assertNull(row.get("description"));
        }

        @Test
        void bothTopicIdAndCustomTopic_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("customTopic", "Bóng đá");

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.exactlyOneTopic").value("QUIZ_TOPIC_EXACTLY_ONE"));
            assertQuizCount(0);
        }

        @Test
        void neitherTopicIdNorCustomTopic_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("topicId", null);
            body.put("customTopic", "  ");

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.exactlyOneTopic").value("QUIZ_TOPIC_EXACTLY_ONE"));
            assertQuizCount(0);
        }

        @Test
        void skillTopicPairNotInSkillTopics_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("topicId", listeningOnlyTopicId); // topic không gắn với READING

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_SKILL_TOPIC_MISMATCH.getCode()));
            assertQuizCount(0);
        }

        @Test
        void formatOfAnotherSkill_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("formatId", listeningFormatId);

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_FORMAT_INVALID.getCode()));
        }

        @Test
        void invalidFields_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("title", "   ");
            body.put("difficulty", 6);
            body.put("skillId", null);

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").value("QUIZ_TITLE_BLANK"))
                    .andExpect(jsonPath("$.errors.difficulty").value("QUIZ_DIFFICULTY_OUT_OF_RANGE"))
                    .andExpect(jsonPath("$.errors.skillId").value("SKILL_BLANK"));
        }

        @Test
        void titleLongerThan200AfterTrim_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("title", "  " + "a".repeat(201) + "  ");

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.titleSizeValid").value("QUIZ_TITLE_SIZE"));
        }

        @Test
        void unknownSkill_returns400() throws Exception {
            Map<String, Object> body = validBody();
            body.put("skillId", 999);
            body.put("topicId", null);
            body.put("formatId", null);
            body.put("customTopic", "Bóng đá");

            mockMvc.perform(json(post("/api/v1/quizzes"), body).with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_SKILL_INVALID.getCode()));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(json(post("/api/v1/quizzes"), validBody())).andExpect(status().isUnauthorized());
        }
    }

    // ===================== GET list =====================

    @Nested
    class GetList {
        @Test
        void filterBySkillAndKeyword_returnsCorrectItemsAndPaging() throws Exception {
            long q1 = insertQuiz(userA, readingId, "Present Simple 1", "READY");
            long q2 = insertQuiz(userA, readingId, "present simple 2", "READY");
            long q3 = insertQuiz(userA, readingId, "PRESENT continuous", "READY");
            insertQuiz(userA, readingId, "Past Simple", "READY"); // sai keyword
            insertQuiz(userA, listeningId, "Present listening", "READY"); // sai skill
            insertQuiz(userB, readingId, "Present of user B", "READY"); // của người khác
            insertQuestion(q1, 1);
            insertQuestion(q1, 2);

            mockMvc.perform(get("/api/v1/quizzes")
                            .param("skillId", String.valueOf(readingId))
                            .param("keyword", "present")
                            .param("size", "2")
                            .param("page", "0")
                            .param("sort", "createdAt,asc")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(2))
                    .andExpect(jsonPath("$.data.content[*].id").value(contains((int) q1, (int) q2)))
                    .andExpect(jsonPath("$.data.content[0].questionCount").value(2))
                    .andExpect(jsonPath("$.data.content[1].questionCount").value(0));

            mockMvc.perform(get("/api/v1/quizzes")
                            .param("skillId", String.valueOf(readingId))
                            .param("keyword", "present")
                            .param("size", "2")
                            .param("page", "1")
                            .param("sort", "createdAt,asc")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(q3));
        }

        @Test
        void defaultSort_isCreatedAtDesc_andOnlyOwnQuizzes() throws Exception {
            long older = insertQuiz(userA, readingId, "Older", "READY");
            jdbc.update("UPDATE quizzes SET created_at = now() - interval '1 day' WHERE id = ?", older);
            long newer = insertQuiz(userA, readingId, "Newer", "READY");
            insertQuiz(userB, readingId, "Of B", "READY");

            mockMvc.perform(get("/api/v1/quizzes").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.content[*].id").value(contains((int) newer, (int) older)));
        }

        @Test
        void filterByStatus() throws Exception {
            insertQuiz(userA, readingId, "Ready", "READY");
            long failed = insertQuiz(userA, readingId, "Failed", "FAILED");

            mockMvc.perform(get("/api/v1/quizzes").param("status", "FAILED").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(contains((int) failed)));
        }

        @Test
        void sizeOver100_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes")
                            .param("size", "101")
                            .param("page", "-1")
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"))
                    .andExpect(jsonPath("$.errors.page").value("PAGE_INVALID"));
        }

        @Test
        void invalidStatus_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes").param("status", "UNKNOWN").with(as(userA)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidSort_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes").param("sort", "password,asc").with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_SORT_INVALID.getCode()));
        }
    }

    // ===================== GET detail =====================

    @Nested
    class Detail {
        @Test
        void ownQuiz_returns200WithQuestionCount() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Mine", "READY");
            insertQuestion(quizId, 1);

            mockMvc.perform(get("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(quizId))
                    .andExpect(jsonPath("$.data.skill.name").value("Reading"))
                    .andExpect(jsonPath("$.data.questionCount").value(1));
        }

        @Test
        void quizOfAnotherUser_returns403() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Of A", "READY");

            mockMvc.perform(get("/api/v1/quizzes/{id}", quizId).with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/quizzes/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }
    }

    // ===================== PUT =====================

    @Nested
    class Update {
        @Test
        void owner_returns200AndUpdates() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Old", "READY");
            Map<String, Object> body = validBody();
            body.put("title", "  New title  ");
            body.put("topicId", null);
            body.put("customTopic", " Bóng đá ");
            body.put("formatId", null);
            body.put("difficulty", 5);

            mockMvc.perform(json(put("/api/v1/quizzes/{id}", quizId), body).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("New title"))
                    .andExpect(jsonPath("$.data.topic").doesNotExist())
                    .andExpect(jsonPath("$.data.customTopic").value("Bóng đá"))
                    .andExpect(jsonPath("$.data.difficulty").value(5))
                    .andExpect(jsonPath("$.data.createdBy").value(userA));

            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM quizzes WHERE id = ?", quizId);
            assertEquals("New title", row.get("title"));
            assertNull(row.get("topic_id"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Of A", "READY");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}", quizId), validBody())
                            .with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            assertEquals("Of A", jdbc.queryForObject("SELECT title FROM quizzes WHERE id = ?", String.class, quizId));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Generating", "GENERATING");

            mockMvc.perform(json(put("/api/v1/quizzes/{id}", quizId), validBody())
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(json(put("/api/v1/quizzes/{id}", 999_999), validBody())
                            .with(as(userA)))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== DELETE =====================

    @Nested
    class Delete {
        @Test
        void owner_returns204_deletesChildren_andKeepsFlashcard() throws Exception {
            long quizId = insertQuiz(userA, readingId, "To delete", "READY");
            long passageId = jdbc.queryForObject(
                    "INSERT INTO passages (quiz_id, position, content) VALUES (?, 1, 'Tom gets up') RETURNING id",
                    Long.class,
                    quizId);
            long questionId = insertQuestion(quizId, 1);
            jdbc.update("UPDATE questions SET passage_id = ? WHERE id = ?", passageId, questionId);
            jdbc.update(
                    "INSERT INTO options (question_id, position, content, is_correct) VALUES (?, 1, 'A', true)",
                    questionId);
            long flashcardId = jdbc.queryForObject(
                    "INSERT INTO flashcard (front_content, back_content, source_passage_id) "
                            + "VALUES ('get up', 'thức dậy', ?) RETURNING id",
                    Long.class,
                    passageId);

            mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertQuizCount(0);
            assertEquals(0, count("questions"));
            assertEquals(0, count("options"));
            assertEquals(0, count("passages"));
            Map<String, Object> flashcard = jdbc.queryForMap("SELECT * FROM flashcard WHERE id = ?", flashcardId);
            assertNull(flashcard.get("source_passage_id"));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Of A", "READY");

            mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId).with(as(userB)))
                    .andExpect(status().isForbidden());
            assertQuizCount(1);
        }

        @Test
        void quizWithAttempts_returns409_andKeepsData() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Attempted", "READY");
            long questionId = insertQuestion(quizId, 1);
            jdbc.update(
                    "INSERT INTO options (question_id, position, content, is_correct) VALUES (?, 1, 'A', true)",
                    questionId);
            jdbc.update("INSERT INTO quiz_attempts (quiz_id, user_id) VALUES (?, ?)", quizId, userB);

            mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));

            assertQuizCount(1);
            assertEquals(1, count("questions"));
            assertEquals(1, count("options"));
            assertEquals(1, count("quiz_attempts"));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long quizId = insertQuiz(userA, readingId, "Generating", "GENERATING");

            mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId).with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
            assertQuizCount(1);
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/quizzes/{id}", 999_999).with(as(userA)))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== Swagger =====================

    @Test
    void swagger_documentsAllFiveEndpointsWith400Examples() throws Exception {
        String base = "$.paths['/api/v1/quizzes']";
        String byId = "$.paths['/api/v1/quizzes/{id}']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(base + ".post", notNullValue()))
                .andExpect(jsonPath(base + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".put", notNullValue()))
                .andExpect(jsonPath(byId + ".delete", notNullValue()))
                .andExpect(jsonPath(
                        base + ".post.responses['400'].content['*/*'].examples['Sai validation']", notNullValue()))
                .andExpect(jsonPath(
                        base + ".post.responses['400'].content['*/*'].examples['Lỗi nghiệp vụ']", notNullValue()))
                .andExpect(jsonPath(
                        byId + ".put.responses['400'].content['*/*'].examples['Sai validation']", notNullValue()))
                .andExpect(
                        jsonPath(base + ".get.responses['400'].content['*/*'].examples['Sai sort']", notNullValue()));
    }

    // ===================== Helpers =====================

    Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Present Simple - Daily routines");
        body.put("description", "Luyện thì hiện tại đơn qua chủ đề sinh hoạt hằng ngày");
        body.put("skillId", readingId);
        body.put("topicId", readingTopicId);
        body.put("customTopic", null);
        body.put("formatId", readingFormatId);
        body.put("difficulty", 2);
        return body;
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

    long insertTopic(long groupId, String slug, String nameVi, long skillId) {
        long topicId = jdbc.queryForObject(
                "INSERT INTO topics (group_id, slug, name_vi) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                groupId,
                slug,
                nameVi);
        jdbc.update("INSERT INTO skill_topics (skill_id, topic_id) VALUES (?, ?)", skillId, topicId);
        return topicId;
    }

    long insertQuiz(long createdBy, long skillId, String title, String status) {
        return jdbc.queryForObject(
                "INSERT INTO quizzes (created_by, skill_id, custom_topic, difficulty, title, status) "
                        + "VALUES (?, ?, 'custom', 2, ?, ?) RETURNING id",
                Long.class,
                createdBy,
                skillId,
                title,
                status);
    }

    long insertQuestion(long quizId, int position) {
        return jdbc.queryForObject(
                "INSERT INTO questions (quiz_id, position, type, content) "
                        + "VALUES (?, ?, 'SINGLE_CHOICE', 'Question?') RETURNING id",
                Long.class,
                quizId,
                position);
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    void assertQuizCount(long expected) {
        assertEquals(expected, count("quizzes"));
    }
}
