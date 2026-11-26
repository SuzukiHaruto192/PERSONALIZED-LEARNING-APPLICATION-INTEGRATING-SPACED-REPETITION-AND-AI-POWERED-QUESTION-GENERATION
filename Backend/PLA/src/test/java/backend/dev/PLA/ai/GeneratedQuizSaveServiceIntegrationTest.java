package backend.dev.PLA.ai;

import static org.assertj.core.api.Assertions.assertThat;

import backend.dev.PLA.dto.ai.GeneratedOption;
import backend.dev.PLA.dto.ai.GeneratedPassage;
import backend.dev.PLA.dto.ai.GeneratedQuestion;
import backend.dev.PLA.dto.ai.GeneratedQuiz;
import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.services.ai.GeneratedQuizSaveService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Kiểm tra lưu quiz AI sinh trên Postgres thật: đúng các ràng buộc FK kép, unique position, CHECK của schema. */
@SpringBootTest
@Testcontainers
class GeneratedQuizSaveServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    GeneratedQuizSaveService saveService;

    @Autowired
    JdbcTemplate jdbc;

    long userId;
    short readingId;
    long topicId;

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE users, topic_groups, flashcard RESTART IDENTITY CASCADE");
        userId = jdbc.queryForObject(
                "INSERT INTO users (username, password) VALUES ('u', 'x') RETURNING id", Long.class);
        readingId = jdbc.queryForObject("SELECT id FROM skills WHERE code = 'READING'", Short.class);
        long group = jdbc.queryForObject(
                "INSERT INTO topic_groups (type, name) VALUES ('SITUATION', 'Daily life') RETURNING id", Long.class);
        topicId = jdbc.queryForObject(
                "INSERT INTO topics (group_id, slug, name_vi) VALUES (?, 'daily', 'Thói quen') RETURNING id",
                Long.class,
                group);
        jdbc.update("INSERT INTO skill_topics (skill_id, topic_id) VALUES (?, ?)", readingId, topicId);
    }

    private static GeneratedQuestion single(String content) {
        return GeneratedQuestion.builder()
                .type(QuestionType.SINGLE_CHOICE)
                .content(content)
                .explanation("vì ...")
                .options(List.of(
                        new GeneratedOption("go", false),
                        new GeneratedOption("goes", true),
                        new GeneratedOption("going", false)))
                .build();
    }

    private static GeneratedQuestion fill(String content) {
        return GeneratedQuestion.builder()
                .type(QuestionType.FILL_BLANK)
                .content(content)
                .correctText(" likes ")
                .options(List.of())
                .build();
    }

    @Test
    void save_writesQuizPassagesQuestionsOptions() {
        GeneratedQuiz generated = GeneratedQuiz.builder()
                .description("Mô tả AI")
                .passages(List.of(
                        new GeneratedPassage("Tom gets up at 6.", List.of(single("Q1"), fill("Q2 ___"))),
                        new GeneratedPassage("Anna walks to work.", List.of(single("Q3")))))
                .build();
        QuizRequest request = QuizRequest.builder()
                .title(" Daily routine ")
                .skillId(readingId)
                .topicId(topicId)
                .build();

        Long quizId = saveService.save(generated, request, userId, (short) 3);

        Map<String, Object> quiz = jdbc.queryForMap("SELECT * FROM quizzes WHERE id = ?", quizId);
        assertThat(quiz)
                .containsEntry("title", "Daily routine") // title người dùng nhập, đã trim
                .containsEntry("description", "Mô tả AI")
                .containsEntry("status", "READY")
                .containsEntry("difficulty", 3) // cột SMALLINT, JDBC trả về Integer
                .containsEntry("created_by", userId)
                .containsEntry("topic_id", topicId);
        assertThat(quiz.get("custom_topic")).isNull();

        assertThat(jdbc.queryForList(
                        "SELECT position FROM passages WHERE quiz_id = ? ORDER BY position", Short.class, quizId))
                .containsExactly((short) 1, (short) 2);
        // Câu hỏi đánh số liên tục trong quiz và gắn đúng passage
        assertThat(jdbc.queryForList(
                        "SELECT q.position || ':' || p.position || ':' || q.type FROM questions q "
                                + "JOIN passages p ON p.id = q.passage_id WHERE q.quiz_id = ? ORDER BY q.position",
                        String.class,
                        quizId))
                .containsExactly("1:1:SINGLE_CHOICE", "2:1:FILL_BLANK", "3:2:SINGLE_CHOICE");
        assertThat(jdbc.queryForObject(
                        "SELECT correct_text FROM questions WHERE quiz_id = ? AND type = 'FILL_BLANK'",
                        String.class,
                        quizId))
                .isEqualTo("likes");
        // 2 câu SINGLE_CHOICE x 3 đáp án, mỗi câu đúng 1 đáp án
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM options o JOIN questions q ON q.id = o.question_id WHERE q.quiz_id = ?",
                        Integer.class,
                        quizId))
                .isEqualTo(6);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM options o JOIN questions q ON q.id = o.question_id "
                                + "WHERE q.quiz_id = ? AND o.is_correct",
                        Integer.class,
                        quizId))
                .isEqualTo(2);
    }

    @Test
    void save_customTopic_storesCustomTopicWithoutTopicId() {
        GeneratedQuiz generated = GeneratedQuiz.builder()
                .passages(List.of(new GeneratedPassage("Text", List.of(fill("Q ___")))))
                .build();
        QuizRequest request = QuizRequest.builder()
                .title("t")
                .skillId(readingId)
                .customTopic("  Du lịch  ")
                .description("Mô tả người dùng")
                .build();

        Long quizId = saveService.save(generated, request, userId, (short) 2);

        Map<String, Object> quiz = jdbc.queryForMap("SELECT * FROM quizzes WHERE id = ?", quizId);
        assertThat(quiz.get("topic_id")).isNull();
        assertThat(quiz)
                .containsEntry("custom_topic", "Du lịch")
                // AI không trả description thì dùng description của người dùng
                .containsEntry("description", "Mô tả người dùng");
    }

    @Test
    void save_shufflesOptions_correctAnswerNotAlwaysAtSamePosition() {
        // AI luôn đặt đáp án đúng ở vị trí 2 ("goes"); sau khi xáo, 30 câu khó có thể (xác suất ~3^-29) cùng một vị trí
        List<GeneratedQuestion> questions = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> single("Q" + i))
                .toList();
        GeneratedQuiz generated = GeneratedQuiz.builder()
                .passages(List.of(new GeneratedPassage("Text", questions)))
                .build();
        QuizRequest request = QuizRequest.builder()
                .title("t")
                .skillId(readingId)
                .topicId(topicId)
                .build();

        Long quizId = saveService.save(generated, request, userId, (short) 2);

        List<Short> correctPositions = jdbc.queryForList(
                "SELECT o.position FROM options o JOIN questions q ON q.id = o.question_id "
                        + "WHERE q.quiz_id = ? AND o.is_correct",
                Short.class,
                quizId);
        assertThat(correctPositions).hasSize(30);
        assertThat(new java.util.HashSet<>(correctPositions)).hasSizeGreaterThan(1);
        // xáo chỉ đổi thứ tự: mỗi câu vẫn đủ 3 đáp án, position 1..3
        assertThat(jdbc.queryForList(
                        "SELECT DISTINCT o.position FROM options o JOIN questions q ON q.id = o.question_id "
                                + "WHERE q.quiz_id = ? ORDER BY o.position",
                        Short.class,
                        quizId))
                .containsExactly((short) 1, (short) 2, (short) 3);
        // list của GeneratedQuestion không bị sửa
        assertThat(questions.get(0).getOptions().get(1).getContent()).isEqualTo("goes");
    }
}
