package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.question.OptionRequest;
import backend.dev.PLA.dto.request.question.QuestionRequest;
import backend.dev.PLA.dto.request.question.QuizRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class QuestionDtoValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void quizRequest_acceptsTopicOrCustomTopic() {
        assertTrue(messages(QuizRequest.builder()
                        .skillId((short) 1)
                        .topicId(1L)
                        .title("quiz")
                        .build())
                .isEmpty());
        assertTrue(messages(QuizRequest.builder()
                        .skillId((short) 1)
                        .customTopic("bong da")
                        .title("quiz")
                        .build())
                .isEmpty());
    }

    @Test
    void quizRequest_rejectsMissingFields() {
        assertEquals(
                Set.of("SKILL_BLANK", "QUIZ_TITLE_BLANK", "TOPIC_BLANK"),
                messages(QuizRequest.builder().customTopic(" ").build()));
    }

    @Test
    void quizRequest_rejectsDifficultyOutOfRange() {
        assertEquals(
                Set.of("QUIZ_DIFFICULTY_RANGE"),
                messages(QuizRequest.builder()
                        .skillId((short) 1)
                        .topicId(1L)
                        .title("quiz")
                        .difficulty((short) 4)
                        .build()));
    }

    @Test
    void questionRequest_rejectsMissingFields() {
        assertEquals(
                Set.of("QUIZ_ID_BLANK", "POSITION_BLANK", "QUESTION_CONTENT_BLANK"),
                messages(QuestionRequest.builder().content("").build()).stream()
                        .filter(m -> !m.equals("QUESTION_CONTENT_PATTERN"))
                        .collect(Collectors.toSet()));
    }

    @Test
    void optionRequest_rejectsMissingFields() {
        assertEquals(
                Set.of("QUESTION_ID_BLANK", "POSITION_BLANK", "ANSWER_BLANK"),
                messages(OptionRequest.builder().content("yes").build()));
    }
}
