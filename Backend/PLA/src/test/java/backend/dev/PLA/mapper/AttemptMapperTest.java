package backend.dev.PLA.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.response.quiz.AttemptAnswerResponse;
import backend.dev.PLA.dto.response.quiz.AttemptQuestionResponse;
import backend.dev.PLA.dto.response.quiz.AttemptResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.entities.quiz.AttemptAnswer;
import backend.dev.PLA.entities.quiz.AttemptAnswerId;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizAttempt;
import backend.dev.PLA.entities.quiz.Skill;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttemptMapperTest {

    private final Skill reading =
            Skill.builder().id((short) 2).code("READING").name("Reading").build();
    private final Quiz quiz =
            Quiz.builder().id(1L).title("Quiz A").skill(reading).build();
    private final Instant now = Instant.parse("2026-11-18T08:00:00Z");
    private final QuizAttempt attempt = QuizAttempt.builder()
            .id(3L)
            .quiz(quiz)
            .startedAt(now)
            .submittedAt(now)
            .correctCount(1)
            .totalQuestions(3)
            .build();

    private AttemptAnswer answer(long attemptId, long questionId, Long optionId, String text, boolean correct) {
        return AttemptAnswer.builder()
                .id(new AttemptAnswerId(attemptId, questionId))
                .selectedOption(
                        optionId == null ? null : Option.builder().id(optionId).build())
                .answerText(text)
                .isCorrect(correct)
                .build();
    }

    @Test
    void toAttemptResponse_mapsSummaryWithoutQuestions() {
        AttemptResponse response = AttemptMapper.toAttemptResponse(attempt);

        assertEquals(3L, response.getId());
        assertEquals(1L, response.getQuiz().getId());
        assertEquals("Quiz A", response.getQuiz().getName());
        assertEquals(2L, response.getSkill().getId());
        assertEquals("Reading", response.getSkill().getName());
        assertEquals(now, response.getStartedAt());
        assertEquals(now, response.getSubmittedAt());
        assertEquals(1, response.getCorrectCount());
        assertEquals(3, response.getTotalQuestions());
        assertNull(response.getQuestions());
    }

    @Test
    void toAttemptResponse_quizWithoutSkill_skillIsNull() {
        quiz.setSkill(null);
        assertNull(AttemptMapper.toAttemptResponse(attempt).getSkill());
    }

    @Test
    void toAttemptAnswerResponse_mapsIdsAndOption() {
        AttemptAnswerResponse withOption = AttemptMapper.toAttemptAnswerResponse(answer(3, 15, 31L, null, true));
        assertEquals(3L, withOption.getAttemptId());
        assertEquals(15L, withOption.getQuestionId());
        assertEquals(31L, withOption.getSelectedOptionId());
        assertNull(withOption.getAnswerText());
        assertTrue(withOption.getIsCorrect());

        AttemptAnswerResponse skipped = AttemptMapper.toAttemptAnswerResponse(answer(3, 17, null, null, false));
        assertNull(skipped.getSelectedOptionId());
        assertFalse(skipped.getIsCorrect());
    }

    @Test
    void toAttemptHistoryResponse_pairsEveryQuestionWithItsAnswer() {
        QuestionResponse q15 = QuestionResponse.builder().id(15L).build();
        QuestionResponse q16 = QuestionResponse.builder().id(16L).build();
        QuestionResponse q17 = QuestionResponse.builder().id(17L).build();

        AttemptResponse response = AttemptMapper.toAttemptHistoryResponse(
                attempt,
                List.of(q15, q16, q17),
                List.of(
                        answer(3, 16, null, "goes", true),
                        answer(3, 15, 31L, null, false),
                        // Câu trả lời của lượt khác phải bị bỏ qua
                        answer(99, 17, 40L, null, true)));

        List<AttemptQuestionResponse> questions = response.getQuestions();
        assertEquals(3, questions.size());
        assertSame(q15, questions.get(0).getQuestion());
        assertEquals(31L, questions.get(0).getAnswer().getSelectedOptionId());
        assertSame(q16, questions.get(1).getQuestion());
        assertEquals("goes", questions.get(1).getAnswer().getAnswerText());
        assertSame(q17, questions.get(2).getQuestion());
        assertNull(questions.get(2).getAnswer());
    }

    @Test
    void toAttemptHistoryResponse_noAnswers_allAnswersNull() {
        AttemptResponse response = AttemptMapper.toAttemptHistoryResponse(
                attempt, List.of(QuestionResponse.builder().id(15L).build()), List.of());

        assertEquals(1, response.getQuestions().size());
        assertNull(response.getQuestions().get(0).getAnswer());
    }
}
