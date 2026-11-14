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

/** Integration test cho 6 endpoint question trên Postgres thật (schema do Flyway tạo). */
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

    // ===================== POST =====================

    @Nested
    class Create {
        @Test
        void threeQuestionsWithoutPosition_getPositions1To3() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            for (int expected = 1; expected <= 3; expected++) {
                mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                                .with(as(userA)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id", notNullValue()))
                        .andExpect(jsonPath("$.data.quizId").value(quizId))
                        .andExpect(jsonPath("$.data.position").value(expected))
                        .andExpect(jsonPath("$.data.type").value("SINGLE_CHOICE"))
                        .andExpect(jsonPath("$.data.options", hasSize(0)))
                        .andExpect(jsonPath("$.data.isComplete").value(false));
            }
            assertEquals(3, count("questions"));
        }

        @Test
        void contentWithQuestionMarkAndVietnamese_returns201_andTrims() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            Map<String, Object> body = singleChoice("  Tom thức dậy lúc mấy giờ? (What time?)  ");
            body.put("explanation", "   ");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.content").value("Tom thức dậy lúc mấy giờ? (What time?)"))
                    .andExpect(jsonPath("$.data.explanation").doesNotExist());

            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM questions WHERE quiz_id = ?", quizId);
            assertEquals("Tom thức dậy lúc mấy giờ? (What time?)", row.get("content"));
            assertNull(row.get("explanation"));
        }

        @Test
        void fillBlankWithCorrectText_returns201_andIsComplete() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), fillBlank("  goes "))
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.type").value("FILL_BLANK"))
                    .andExpect(jsonPath("$.data.correctText").value("goes"))
                    .andExpect(jsonPath("$.data.isComplete").value(true));
        }

        @Test
        void fillBlankWithoutCorrectText_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), fillBlank("   "))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.correctTextValid").value("QUESTION_CORRECT_TEXT_INVALID"));
            assertEquals(0, count("questions"));
        }

        @Test
        void singleChoiceWithCorrectText_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            Map<String, Object> body = singleChoice("Q?");
            body.put("correctText", "goes");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.correctTextValid").value("QUESTION_CORRECT_TEXT_INVALID"));
        }

        @Test
        void correctTextTooLong_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), fillBlank("a".repeat(256)))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.correctTextSizeValid").value("CORRECT_TEXT_SIZE"));
        }

        @Test
        void missingTypeAndBlankContent_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            Map<String, Object> body = singleChoice("   ");
            body.put("type", null);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.type").value("QUESTION_TYPE_BLANK"))
                    .andExpect(jsonPath("$.errors.content").value("QUESTION_CONTENT_BLANK"));
        }

        @Test
        void unknownType_returns1060() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            Map<String, Object> body = singleChoice("Q?");
            body.put("type", "ESSAY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST_BODY.getCode()));
        }

        @Test
        void positionOutOfRange_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            Map<String, Object> body = singleChoice("Q?");
            body.put("position", 0);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.position").value("QUESTION_POSITION_INVALID"));
        }

        @Test
        void duplicatedPosition_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            insertQuestion(quizId, 3, null);
            Map<String, Object> body = singleChoice("Q?");
            body.put("position", 3);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_POSITION_DUPLICATED.getCode()));
            assertEquals(1, count("questions"));
        }

        @Test
        void passageOfAnotherQuiz_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long otherPassage = insertPassage(insertQuiz(userA, "READY"), 1);
            Map<String, Object> body = singleChoice("Q?");
            body.put("passageId", otherPassage);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_NOT_IN_QUIZ.getCode()));
            assertEquals(0, count("questions"));
        }

        @Test
        void passageOfSameQuiz_filterReturnsIt_andPassageQuestionCountIncreases() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            insertQuestion(quizId, 1, null);
            Map<String, Object> body = singleChoice("About the passage?");
            body.put("passageId", passageId);

            String response = mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.passageId").value(passageId))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            int questionId = objectMapper.readTree(response).at("/data/id").asInt();

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", quizId)
                            .param("passageId", String.valueOf(passageId))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id", contains(questionId)));

            mockMvc.perform(get("/api/v1/passages/{id}", passageId).with(as(userA)))
                    .andExpect(jsonPath("$.data.questionCount").value(1));
        }

        @Test
        void grammarTopic_returns201WithRef() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long topicId = insertTopic("GRAMMAR", "present-simple", "Thì hiện tại đơn");
            Map<String, Object> body = singleChoice("Q?");
            body.put("grammarTopicId", topicId);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.grammarTopic.id").value(topicId))
                    .andExpect(jsonPath("$.data.grammarTopic.name").value("Thì hiện tại đơn"));
        }

        @Test
        void situationTopicAsGrammarTopic_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long topicId = insertTopic("SITUATION", "airport", "Sân bay");
            Map<String, Object> body = singleChoice("Q?");
            body.put("grammarTopicId", topicId);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_GRAMMAR_TOPIC_INVALID.getCode()));

            body.put("grammarTopicId", 999_999);
            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_GRAMMAR_TOPIC_INVALID.getCode()));
        }

        @Test
        void question101_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            jdbc.update(
                    "INSERT INTO questions (quiz_id, position, type, content) "
                            + "SELECT ?, g, 'SINGLE_CHOICE', 'Q' FROM generate_series(1, 100) g",
                    quizId);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_QUESTION_LIMIT.getCode()));
            assertEquals(100, count("questions"));
        }

        @Test
        void otherUsersQuiz_returns403() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                            .with(as(userB)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            assertEquals(0, count("questions"));
        }

        @Test
        void quizNotExisting_returns404() throws Exception {
            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", 999_999), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_NOT_FOUND.getCode()));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long quizId = insertQuiz(userA, "GENERATING");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
        }

        @Test
        void quizWithAttempts_canStillAddQuestion() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            insertAttempt(quizId);

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isCreated());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?")))
                    .andExpect(status().isUnauthorized());
        }
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

    // ===================== PUT =====================

    @Nested
    class Update {
        @Test
        void updateContent_withoutPosition_keepsPosition_andDetachesPassage() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long passageId = insertPassage(quizId, 1);
            long questionId = insertQuestion(quizId, 4, passageId);
            Map<String, Object> body = singleChoice("New content?");
            body.put("passageId", null);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), body)
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("New content?"))
                    .andExpect(jsonPath("$.data.position").value(4))
                    .andExpect(jsonPath("$.data.passageId").doesNotExist());

            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM questions WHERE id = ?", questionId);
            assertEquals("New content?", row.get("content"));
            assertNull(row.get("passage_id"));
        }

        @Test
        void updatePosition_toFreeSlot_returns200() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            Map<String, Object> body = singleChoice("Q?");
            body.put("position", 9);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), body)
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.position").value(9));
        }

        @Test
        void duplicatedPosition_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            insertQuestion(quizId, 2, null);
            Map<String, Object> body = singleChoice("Q?");
            body.put("position", 2);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), body)
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_POSITION_DUPLICATED.getCode()));
        }

        @Test
        void samePositionAsItself_returns200() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 2, null);
            Map<String, Object> body = singleChoice("Q?");
            body.put("position", 2);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), body)
                            .with(as(userA)))
                    .andExpect(status().isOk());
        }

        @Test
        void singleChoiceWithOptions_toFillBlank_returns409() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            insertOption(questionId, 1, "A", true);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), fillBlank("goes"))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_HAS_OPTIONS.getCode()));

            assertEquals(
                    "SINGLE_CHOICE",
                    jdbc.queryForObject("SELECT type FROM questions WHERE id = ?", String.class, questionId));
        }

        @Test
        void singleChoiceWithoutOptions_toFillBlank_returns200() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), fillBlank("goes"))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("FILL_BLANK"))
                    .andExpect(jsonPath("$.data.isComplete").value(true));
        }

        @Test
        void quizWithAttempts_canEditContent_butNotTypeOrCorrectText() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long questionId = insertQuestion(quizId, 1, null);
            long fillBlankId = insertFillBlank(quizId, 2, "goes");
            insertAttempt(quizId);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), singleChoice("Edited?"))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Edited?"));

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), fillBlank("goes"))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()))
                    .andExpect(
                            jsonPath("$.message").value("Quiz đã có lượt làm bài, không thể đổi loại câu hoặc đáp án"));

            mockMvc.perform(json(put("/api/v1/questions/{id}", fillBlankId), fillBlank("go"))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_HAS_ATTEMPTS.getCode()));

            assertEquals(
                    "goes",
                    jdbc.queryForObject("SELECT correct_text FROM questions WHERE id = ?", String.class, fillBlankId));
        }

        @Test
        void otherUser_returns403() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "READY"), 1, null);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), singleChoice("Hacked?"))
                            .with(as(userB)))
                    .andExpect(status().isForbidden());
            assertEquals(
                    "Question?",
                    jdbc.queryForObject("SELECT content FROM questions WHERE id = ?", String.class, questionId));
        }

        @Test
        void generatingQuiz_returns409() throws Exception {
            long questionId = insertQuestion(insertQuiz(userA, "GENERATING"), 1, null);

            mockMvc.perform(json(put("/api/v1/questions/{id}", questionId), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.QUIZ_GENERATING.getCode()));
        }

        @Test
        void notExisting_returns404() throws Exception {
            mockMvc.perform(json(put("/api/v1/questions/{id}", 999_999), singleChoice("Q?"))
                            .with(as(userA)))
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

            mockMvc.perform(json(post("/api/v1/quizzes/{quizId}/questions", quizId), singleChoice("Q?"))
                            .with(as(userA)))
                    .andExpect(status().isCreated());
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

    // ===================== PUT order =====================

    @Nested
    class Reorder {
        @Test
        void swapTwoQuestions_getReturnsNewOrder() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long q1 = insertQuestion(quizId, 1, null);
            long q2 = insertQuestion(quizId, 2, null);

            mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), order(q2, q1))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id", contains((int) q2, (int) q1)))
                    .andExpect(jsonPath("$.data[*].position", contains(1, 2)));

            mockMvc.perform(get("/api/v1/quizzes/{quizId}/questions", quizId).with(as(userA)))
                    .andExpect(jsonPath("$.data[*].id", contains((int) q2, (int) q1)));
        }

        @Test
        void fourQuestionsWithGaps_renumbersFrom1() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long q12 = insertQuestion(quizId, 2, null);
            long q13 = insertQuestion(quizId, 5, null);
            long q14 = insertQuestion(quizId, 7, null);
            long q15 = insertQuestion(quizId, 9, null);

            mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), order(q15, q12, q13, q14))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id", contains((int) q15, (int) q12, (int) q13, (int) q14)))
                    .andExpect(jsonPath("$.data[*].position", contains(1, 2, 3, 4)));
        }

        @Test
        void missingExtraOrDuplicatedIds_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long q1 = insertQuestion(quizId, 1, null);
            long q2 = insertQuestion(quizId, 2, null);
            long otherQuizQuestion = insertQuestion(insertQuiz(userA, "READY"), 1, null);

            for (Map<String, Object> body :
                    List.of(order(q1), order(q1, q2, otherQuizQuestion), order(q1, q1), order(q1, q1, q2))) {
                mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), body)
                                .with(as(userA)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_ORDER_INVALID.getCode()));
            }
            assertEquals(1, jdbc.queryForObject("SELECT position FROM questions WHERE id = ?", Integer.class, q1));
            assertEquals(2, jdbc.queryForObject("SELECT position FROM questions WHERE id = ?", Integer.class, q2));
        }

        @Test
        void missingQuestionIds_returns400() throws Exception {
            long quizId = insertQuiz(userA, "READY");

            mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), new HashMap<>())
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.questionIds").value("QUESTION_ORDER_INVALID"));
        }

        @Test
        void quizWithAttempts_canReorder() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long q1 = insertQuestion(quizId, 1, null);
            long q2 = insertQuestion(quizId, 2, null);
            insertAttempt(quizId);

            mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), order(q2, q1))
                            .with(as(userA)))
                    .andExpect(status().isOk());
        }

        @Test
        void otherUser_returns403() throws Exception {
            long quizId = insertQuiz(userA, "READY");
            long q1 = insertQuestion(quizId, 1, null);

            mockMvc.perform(json(put("/api/v1/quizzes/{quizId}/questions/order", quizId), order(q1))
                            .with(as(userB)))
                    .andExpect(status().isForbidden());
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
    void swagger_documentsAllSixEndpoints() throws Exception {
        String byQuiz = "$.paths['/api/v1/quizzes/{quizId}/questions']";
        String byId = "$.paths['/api/v1/questions/{id}']";
        String order = "$.paths['/api/v1/quizzes/{quizId}/questions/order']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(byQuiz + ".post", notNullValue()))
                .andExpect(jsonPath(byQuiz + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".get", notNullValue()))
                .andExpect(jsonPath(byId + ".put", notNullValue()))
                .andExpect(jsonPath(byId + ".delete", notNullValue()))
                .andExpect(jsonPath(order + ".put", notNullValue()))
                .andExpect(jsonPath(
                        byQuiz + ".post.responses['400'].content['*/*'].examples['Sai validation']", notNullValue()))
                .andExpect(jsonPath(
                        byId + ".put.responses['409'].content['*/*'].examples['Còn options']", notNullValue()));
    }

    // ===================== Helpers =====================

    Map<String, Object> singleChoice(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("passageId", null);
        body.put("position", null);
        body.put("type", "SINGLE_CHOICE");
        body.put("content", content);
        body.put("correctText", null);
        body.put("explanation", null);
        body.put("grammarTopicId", null);
        return body;
    }

    Map<String, Object> fillBlank(String correctText) {
        Map<String, Object> body = singleChoice("She ___ to school every day.");
        body.put("type", "FILL_BLANK");
        body.put("correctText", correctText);
        return body;
    }

    Map<String, Object> order(long... ids) {
        Map<String, Object> body = new HashMap<>();
        body.put("questionIds", java.util.Arrays.stream(ids).boxed().toList());
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
