package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.sql.Date;
import java.time.LocalDate;
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

/** Integration test cho các API flashcard / deck mới trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FlashCardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    final ObjectMapper objectMapper = new ObjectMapper();

    final LocalDate today = LocalDate.now();

    long userA;
    long userB;

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
    }

    // ===================== due =====================

    @Nested
    class Due {
        @Test
        void overdueFirst_paged_noNewCards_andOnlyMyCards() throws Exception {
            long deck = insertDeck(userA, "deck");
            long overdue = insertCard(userA, "overdue");
            long overdue2 = insertCard(userA, "overdue2");
            long dueToday = insertCard(userA, "today");
            long future = insertCard(userA, "future");
            long new1 = insertCard(userA, "new1");
            insertReviewedDetail(deck, dueToday, today);
            insertReviewedDetail(deck, overdue, today.minusDays(5));
            insertReviewedDetail(deck, overdue2, today.minusDays(2));
            insertReviewedDetail(deck, future, today.plusDays(3));
            insertNewDetail(deck, new1, "2026-01-01T00:00:00Z");
            // thẻ đến hạn của user khác không được lẫn vào
            long deckB = insertDeck(userB, "deck");
            insertReviewedDetail(deckB, insertCard(userB, "other"), today.minusDays(10));

            mockMvc.perform(get("/api/v1/flashcards/due")
                            .param("page", "0")
                            .param("size", "2")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(2))
                    .andExpect(jsonPath("$.data.content[*].flashCardId").value(contains((int) overdue, (int) overdue2)))
                    .andExpect(jsonPath("$.data.content[0].overdueDays").value(5));

            // trang cuối: chỉ còn thẻ đến hạn hôm nay, không có thẻ mới / thẻ chưa tới hạn
            mockMvc.perform(get("/api/v1/flashcards/due")
                            .param("page", "1")
                            .param("size", "2")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].flashCardId").value(contains((int) dueToday)));
        }

        @Test
        void filterByDeck_andDeckOfAnotherUser_returns1046() throws Exception {
            long deck1 = insertDeck(userA, "deck 1");
            long deck2 = insertDeck(userA, "deck 2");
            long card = insertCard(userA, "apple");
            insertReviewedDetail(deck1, card, today);
            insertNewDetail(deck2, card, "2026-01-01T00:00:00Z");

            mockMvc.perform(get("/api/v1/flashcards/due")
                            .param("deckId", String.valueOf(deck2))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));

            mockMvc.perform(get("/api/v1/flashcards/due")
                            .param("deckId", String.valueOf(deck1))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].deckId").value(contains((int) deck1)));

            mockMvc.perform(get("/api/v1/flashcards/due")
                            .param("deckId", String.valueOf(deck1))
                            .with(as(userB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
        }
    }

    // ===================== deck =====================

    @Nested
    class Deck {
        @Test
        void detail_returnsStats() throws Exception {
            long deck = insertDeck(userA, "deck");
            insertReviewedDetail(deck, insertCard(userA, "a"), today.minusDays(1));
            insertReviewedDetail(deck, insertCard(userA, "b"), today.plusDays(2));
            insertNewDetail(deck, insertCard(userA, "c"), "2026-01-01T00:00:00Z");

            mockMvc.perform(get("/api/v1/decks/{id}", deck).with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("deck"))
                    .andExpect(jsonPath("$.data.stats.totalCards").value(3))
                    .andExpect(jsonPath("$.data.stats.dueToday").value(1))
                    .andExpect(jsonPath("$.data.stats.newCards").value(1))
                    .andExpect(jsonPath("$.data.stats.review").value(2))
                    .andExpect(jsonPath("$.data.stats.learning").value(0));

            mockMvc.perform(get("/api/v1/decks/{id}", deck).with(as(userB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
        }

        @Test
        void update_sameNameAllowed_duplicateNameRejected() throws Exception {
            long deck = insertDeck(userA, "deck 1");
            insertDeck(userA, "deck 2");

            mockMvc.perform(json(put("/api/v1/decks/{id}", deck), Map.of("name", "deck 1", "description", "mo ta"))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description").value("mo ta"));

            mockMvc.perform(json(put("/api/v1/decks/{id}", deck), Map.of("name", " deck 2 "))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DECK_EXISTED.getCode()));

            mockMvc.perform(json(put("/api/v1/decks/{id}", deck), Map.of("name", "x"))
                            .with(as(userB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
        }

        @Test
        void delete_removesOrphanCards_keepsCardsStillInOtherDecks() throws Exception {
            long deck1 = insertDeck(userA, "deck 1");
            long deck2 = insertDeck(userA, "deck 2");
            long onlyHere = insertCard(userA, "only");
            long shared = insertCard(userA, "shared");
            insertNewDetail(deck1, onlyHere, "2026-01-01T00:00:00Z");
            insertNewDetail(deck1, shared, "2026-01-01T00:00:00Z");
            insertNewDetail(deck2, shared, "2026-01-01T00:00:00Z");

            mockMvc.perform(delete("/api/v1/decks/{id}", deck1).with(as(userA))).andExpect(status().isNoContent());

            assertEquals(0, count("SELECT COUNT(*) FROM flashcard_decks WHERE id = ?", deck1));
            assertEquals(0, count("SELECT COUNT(*) FROM flashcard WHERE id = ?", onlyHere));
            assertEquals(1, count("SELECT COUNT(*) FROM flashcard WHERE id = ?", shared));
        }

        @Test
        void deckFlashCards_paginatedWithSchedule() throws Exception {
            long deck = insertDeck(userA, "deck");
            insertNewDetail(deck, insertCard(userA, "a"), "2026-01-01T00:00:00Z");
            insertNewDetail(deck, insertCard(userA, "b"), "2026-01-02T00:00:00Z");

            mockMvc.perform(get("/api/v1/decks/{id}/flashcards", deck)
                            .param("size", "1")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].frontContent").value("a"))
                    .andExpect(jsonPath("$.data.content[0].fsrsState").value("NEW"))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }
    }

    // ===================== sửa / xoá thẻ =====================

    @Nested
    class EditCard {
        @Test
        void update_notShared_editsInPlace() throws Exception {
            long deck = insertDeck(userA, "deck");
            long card = insertCard(userA, "apple");
            insertNewDetail(deck, card, "2026-01-01T00:00:00Z");

            mockMvc.perform(json(put("/api/v1/flashcards/{id}", card), cardBody("pear", "lê", "A pear."))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(card))
                    .andExpect(jsonPath("$.data.frontContent").value("pear"))
                    .andExpect(jsonPath("$.data.example").value("A pear."));
        }

        @Test
        void update_sharedCard_copyOnWrite_keepsBothSchedules() throws Exception {
            long deckA = insertDeck(userA, "deck");
            long deckB = insertDeck(userB, "deck");
            long shared = insertCard(userB, "apple");
            insertReviewedDetail(deckA, shared, today.plusDays(7));
            insertReviewedDetail(deckB, shared, today.plusDays(2));

            String body = mockMvc.perform(json(put("/api/v1/flashcards/{id}", shared), cardBody("pear", "lê", null))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            long newId = objectMapper.readTree(body).at("/data/id").asLong();

            assertNotEquals(shared, newId);
            // user B vẫn thấy nội dung cũ, lịch ôn giữ nguyên
            assertEquals(
                    "apple",
                    jdbc.queryForObject("SELECT front_content FROM flashcard WHERE id = ?", String.class, shared));
            assertEquals(today.plusDays(2), nextReview(deckB, shared));
            // dòng của user A đã chuyển sang thẻ mới, giữ lịch ôn
            assertEquals(today.plusDays(7), nextReview(deckA, newId));
            assertEquals(
                    0,
                    count(
                            "SELECT COUNT(*) FROM flashcard_detail WHERE deck_id = ? AND flashcard_id = ?",
                            deckA,
                            shared));
        }

        @Test
        void update_duplicateContent_returns1121_andNotMine_returns1031() throws Exception {
            long deck = insertDeck(userA, "deck");
            long apple = insertCard(userA, "apple");
            long pear = insertCard(userA, "pear");
            insertNewDetail(deck, apple, "2026-01-01T00:00:00Z");
            insertNewDetail(deck, pear, "2026-01-01T00:00:00Z");

            mockMvc.perform(json(put("/api/v1/flashcards/{id}", apple), cardBody("PEAR", "back of pear", null))
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_CONTENT_EXISTED.getCode()));

            mockMvc.perform(json(put("/api/v1/flashcards/{id}", apple), cardBody("x", "y", null))
                            .with(as(userB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_NOT_FOUND.getCode()));
        }

        @Test
        void delete_sharedCard_staysInDeckOfOtherUser() throws Exception {
            long deckA = insertDeck(userA, "deck");
            long deckB = insertDeck(userB, "deck");
            long shared = insertCard(userA, "apple");
            insertNewDetail(deckA, shared, "2026-01-01T00:00:00Z");
            insertNewDetail(deckB, shared, "2026-01-01T00:00:00Z");

            mockMvc.perform(delete("/api/v1/flashcards/{id}", shared).with(as(userA)))
                    .andExpect(status().isNoContent());

            assertEquals(1, count("SELECT COUNT(*) FROM flashcard WHERE id = ?", shared));
            assertEquals(1, count("SELECT COUNT(*) FROM flashcard_detail WHERE flashcard_id = ?", shared));

            mockMvc.perform(delete("/api/v1/flashcards/{id}", shared).with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_NOT_FOUND.getCode()));
        }

        @Test
        void removeFromDeck_lastDeck_deletesCard() throws Exception {
            long deck = insertDeck(userA, "deck");
            long card = insertCard(userA, "apple");
            insertNewDetail(deck, card, "2026-01-01T00:00:00Z");

            mockMvc.perform(delete("/api/v1/decks/{deckId}/flashcards/{id}", deck, card)
                            .with(as(userA)))
                    .andExpect(status().isNoContent());
            assertEquals(0, count("SELECT COUNT(*) FROM flashcard WHERE id = ?", card));

            mockMvc.perform(delete("/api/v1/decks/{deckId}/flashcards/{id}", deck, card)
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_DETAIL_NOT_FOUND.getCode()));
        }
    }

    // ===================== thêm thẻ / tạo từ passage / reset =====================

    @Nested
    class AddAndReset {
        @Test
        void addExisting_countsAddedSkipped_andInvalidIdAddsNothing() throws Exception {
            long source = insertDeck(userA, "source");
            long target = insertDeck(userA, "target");
            long c1 = insertCard(userA, "a");
            long c2 = insertCard(userA, "b");
            insertNewDetail(source, c1, "2026-01-01T00:00:00Z");
            insertNewDetail(source, c2, "2026-01-01T00:00:00Z");
            insertNewDetail(target, c2, "2026-01-01T00:00:00Z");

            mockMvc.perform(json(post("/api/v1/decks/{id}/flashcards", target), Map.of("flashCardIds", List.of(c1, c2)))
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.added").value(1))
                    .andExpect(jsonPath("$.data.skipped").value(1));

            long cardOfB = insertCard(userB, "b-card");
            insertNewDetail(insertDeck(userB, "deck"), cardOfB, "2026-01-01T00:00:00Z");
            long fresh = insertCard(userA, "c");
            insertNewDetail(source, fresh, "2026-01-01T00:00:00Z");

            mockMvc.perform(json(
                                    post("/api/v1/decks/{id}/flashcards", target),
                                    Map.of("flashCardIds", List.of(fresh, cardOfB)))
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.FLASHCARD_NOT_FOUND.getCode()));
            assertEquals(
                    0,
                    count(
                            "SELECT COUNT(*) FROM flashcard_detail WHERE deck_id = ? AND flashcard_id = ?",
                            target,
                            fresh));
        }

        @Test
        void createFromPassage_savesSourcePassage_andForeignQuizReturns1002() throws Exception {
            long readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
            long quizA = insertQuiz(userA, readingId);
            long quizB = insertQuiz(userB, readingId);
            long passageA = insertPassage(quizA);
            long passageB = insertPassage(quizB);
            long deck = insertDeck(userA, "deck");
            Map<String, Object> body = Map.of(
                    "deckId",
                    deck,
                    "frontContent",
                    "itinerary",
                    "backContent",
                    "lịch trình",
                    "example",
                    "Our itinerary.");

            mockMvc.perform(json(post("/api/v1/passages/{id}/flashcards", passageA), body)
                            .with(as(userA)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.sourcePassageId").value(passageA))
                    .andExpect(jsonPath("$.data.fsrsState").value("NEW"))
                    .andExpect(jsonPath("$.data.example").value("Our itinerary."));

            mockMvc.perform(json(post("/api/v1/passages/{id}/flashcards", passageB), body)
                            .with(as(userA)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));

            mockMvc.perform(json(post("/api/v1/passages/{id}/flashcards", 999_999), body)
                            .with(as(userA)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSAGE_NOT_FOUND.getCode()));
        }

        @Test
        void reset_backToNew_andLeavesDue() throws Exception {
            long deck = insertDeck(userA, "deck");
            long card = insertCard(userA, "apple");
            insertReviewedDetail(deck, card, today.minusDays(1));

            mockMvc.perform(get("/api/v1/flashcards/due").with(as(userA)))
                    .andExpect(jsonPath("$.data.content[0].flashCardId").value(card));

            mockMvc.perform(post("/api/v1/decks/{deckId}/flashcards/{id}/reset", deck, card)
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fsrsState").value("NEW"))
                    .andExpect(jsonPath("$.data.lapses").value(0))
                    .andExpect(jsonPath("$.data.nextReviewDate").doesNotExist());

            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM flashcard_detail WHERE deck_id = ? AND flashcard_id = ?", deck, card);
            assertNull(row.get("stability"));
            assertNull(row.get("difficulty"));
            assertNull(row.get("last_review"));

            mockMvc.perform(get("/api/v1/flashcards/due").with(as(userA)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));

            mockMvc.perform(post("/api/v1/decks/{deckId}/flashcards/{id}/reset", deck, card)
                            .with(as(userB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DECK_NOT_FOUND.getCode()));
        }

        @Test
        void create_doesNotReuseCardOfAnotherUser() throws Exception {
            long deckA = insertDeck(userA, "deck");
            long deckB = insertDeck(userB, "deck");
            long cardOfB = insertCard(userB, "apple");
            insertNewDetail(deckB, cardOfB, "2026-01-01T00:00:00Z");

            mockMvc.perform(json(
                                    post("/api/v1/flashcards"),
                                    Map.of("deckId", deckA, "frontContent", "APPLE", "backContent", "back of apple"))
                            .with(as(userA)))
                    .andExpect(status().isCreated());

            Long cardOfA = jdbc.queryForObject(
                    "SELECT flashcard_id FROM flashcard_detail WHERE deck_id = ?", Long.class, deckA);
            assertNotEquals(cardOfB, cardOfA);
            Map<String, Object> detail = jdbc.queryForMap("SELECT * FROM flashcard_detail WHERE deck_id = ?", deckA);
            assertNull(detail.get("stability"));
            assertNull(detail.get("difficulty"));
        }
    }

    // ===================== helpers =====================

    Map<String, Object> cardBody(String front, String back, String example) {
        return example == null
                ? Map.of("frontContent", front, "backContent", back)
                : Map.of("frontContent", front, "backContent", back, "example", example);
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

    long insertDeck(long userId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO flashcard_decks (user_id, name) VALUES (?, ?) RETURNING id", Long.class, userId, name);
    }

    long insertCard(long createdBy, String front) {
        return jdbc.queryForObject(
                "INSERT INTO flashcard (created_by, front_content, back_content) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                createdBy,
                front,
                "back of " + front);
    }

    void insertNewDetail(long deckId, long cardId, String addedAt) {
        jdbc.update(
                "INSERT INTO flashcard_detail (deck_id, flashcard_id, added_at) VALUES (?, ?, ?::timestamptz)",
                deckId,
                cardId,
                addedAt);
    }

    void insertReviewedDetail(long deckId, long cardId, LocalDate nextReview) {
        jdbc.update(
                "INSERT INTO flashcard_detail (deck_id, flashcard_id, stability, difficulty, fsrs_state, last_review, "
                        + "next_review) VALUES (?, ?, 5.0, 5.0, 'REVIEW', ?, ?)",
                deckId,
                cardId,
                Date.valueOf(nextReview.minusDays(5)),
                Date.valueOf(nextReview));
    }

    long insertQuiz(long createdBy, long skillId) {
        return jdbc.queryForObject(
                "INSERT INTO quizzes (created_by, skill_id, custom_topic, difficulty, title, status) "
                        + "VALUES (?, ?, 'custom', 2, 'Quiz', 'READY') RETURNING id",
                Long.class,
                createdBy,
                skillId);
    }

    long insertPassage(long quizId) {
        return jdbc.queryForObject(
                "INSERT INTO passages (quiz_id, position, content) VALUES (?, 1, 'Our itinerary.') RETURNING id",
                Long.class,
                quizId);
    }

    LocalDate nextReview(long deckId, long cardId) {
        return jdbc.queryForObject(
                        "SELECT next_review FROM flashcard_detail WHERE deck_id = ? AND flashcard_id = ?",
                        Date.class,
                        deckId,
                        cardId)
                .toLocalDate();
    }

    long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
