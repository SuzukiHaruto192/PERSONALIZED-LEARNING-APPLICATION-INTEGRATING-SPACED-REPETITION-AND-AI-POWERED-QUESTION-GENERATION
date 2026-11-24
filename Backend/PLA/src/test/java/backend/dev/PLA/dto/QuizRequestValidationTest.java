package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuizRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private QuizRequest.QuizRequestBuilder valid() {
        return QuizRequest.builder()
                .title("Daily routine")
                .skillId((short) 1)
                .topicId(12L)
                .questionCount(20);
    }

    private Set<String> createMessages(QuizRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 30, 40})
    void questionCountInRange_isValid(int count) {
        assertTrue(createMessages(valid().questionCount(count).build()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 19, 41})
    void questionCountOutOfRange_isInvalid(int count) {
        assertEquals(
                Set.of("QUIZ_QUESTION_COUNT_OUT_OF_RANGE"),
                createMessages(valid().questionCount(count).build()));
    }

    @Test
    void missingQuestionCount_isInvalid() {
        assertEquals(
                Set.of("QUIZ_QUESTION_COUNT_BLANK"),
                createMessages(valid().questionCount(null).build()));
    }
}
