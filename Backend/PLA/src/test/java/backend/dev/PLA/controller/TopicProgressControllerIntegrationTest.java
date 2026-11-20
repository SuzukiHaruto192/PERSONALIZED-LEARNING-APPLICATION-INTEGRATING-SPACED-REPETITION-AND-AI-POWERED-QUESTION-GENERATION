package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import java.sql.Date;
import java.time.LocalDate;
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

/** Integration test cho các API đọc user_topic_progress trên Postgres thật (schema do Flyway tạo). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TopicProgressControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    final LocalDate today = LocalDate.now();

    long userA;
    long userB;
    long readingId;
    long listeningId;
    long groupId;

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userA = insertUser("userA");
        userB = insertUser("userB");
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Long.class);
        listeningId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'LISTENING'", Long.class);
        groupId = jdbc.queryForObject(
                "INSERT INTO topic_groups (type, name) VALUES ('SITUATION', 'Daily life') RETURNING id", Long.class);
    }

    // ===================== GET /progress/due =====================

    @Nested
    class Due {
        @Test
        void onlyDueRows_overdueFirstThenLowMastery_paged_andOnlyMine() throws Exception {
            long travel = insertTopic("travel");
            long food = insertTopic("food");
            long work = insertTopic("work");
            long future = insertTopic("future");
            long fresh = insertTopic("fresh");
            insertProgress(userA, readingId, travel, today.minusDays(4), today.minusDays(10), "80.00");
            // cùng ngày đến hạn: mastery thấp đứng trước
            insertProgress(userA, readingId, food, today, today.minusDays(5), "70.00");
            insertProgress(userA, listeningId, work, today, today.minusDays(5), "40.00");
            insertProgress(userA, readingId, future, today.plusDays(2), today.minusDays(1), "90.00");
            insertProgress(userA, readingId, fresh, null, null, "0.00");
            // dòng đến hạn của user khác không được lẫn vào
            insertProgress(userB, readingId, travel, today.minusDays(30), today.minusDays(40), "10.00");

            mockMvc.perform(get("/api/v1/progress/due")
                            .param("page", "0")
                            .param("size", "2")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.content[*].topic.id").value(contains((int) travel, (int) work)))
                    .andExpect(jsonPath("$.data.content[0].skill.id").value(readingId))
                    .andExpect(jsonPath("$.data.content[0].skill.name").exists())
                    .andExpect(jsonPath("$.data.content[0].topic.slug").value("travel"))
                    .andExpect(jsonPath("$.data.content[0].overdueDays").value(4))
                    // response chỉ gồm skill, topic, overdueDays
                    .andExpect(jsonPath("$.data.content[0].mastery").doesNotExist())
                    .andExpect(jsonPath("$.data.content[0].nextReviewDate").doesNotExist());

            mockMvc.perform(get("/api/v1/progress/due")
                            .param("page", "1")
                            .param("size", "2")
                            .with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].topic.id").value(contains((int) food)))
                    .andExpect(jsonPath("$.data.content[0].overdueDays").value(0));
        }

        @Test
        void nothingDue_returnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/v1/progress/due").with(as(userA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        void invalidPaging_returns1006() throws Exception {
            mockMvc.perform(get("/api/v1/progress/due")
                            .param("page", "-1")
                            .param("size", "101")
                            .with(as(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors.page").value("PAGE_INVALID"))
                    .andExpect(jsonPath("$.errors.size").value("PAGE_SIZE_INVALID"));
        }

        @Test
        void anonymous_isRejected() throws Exception {
            mockMvc.perform(get("/api/v1/progress/due")).andExpect(status().isUnauthorized());
        }
    }

    // ===================== helpers =====================

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

    /** Topic gắn với cả Reading và Listening trong skill_topics (FK của user_topic_progress). */
    long insertTopic(String slug) {
        long topic = jdbc.queryForObject(
                "INSERT INTO topics (group_id, slug, name_vi) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                groupId,
                slug,
                slug);
        jdbc.update(
                "INSERT INTO skill_topics (skill_id, topic_id) VALUES (?, ?), (?, ?)",
                readingId,
                topic,
                listeningId,
                topic);
        return topic;
    }

    void insertProgress(
            long userId, long skillId, long topicId, LocalDate nextReview, LocalDate lastReview, String mastery) {
        boolean reviewed = lastReview != null;
        jdbc.update(
                "INSERT INTO user_topic_progress (user_id, skill_id, topic_id, attempts_count, questions_answered, "
                        + "questions_correct, mastery, stability, difficulty, fsrs_state, last_review, next_review) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?::numeric, ?, ?, ?, ?, ?)",
                userId,
                skillId,
                topicId,
                reviewed ? 1 : 0,
                reviewed ? 10 : 0,
                reviewed ? 5 : 0,
                mastery,
                reviewed ? 5.0 : null,
                reviewed ? 5.0 : null,
                reviewed ? "REVIEW" : "NEW",
                reviewed ? Date.valueOf(lastReview) : null,
                nextReview == null ? null : Date.valueOf(nextReview));
    }
}
