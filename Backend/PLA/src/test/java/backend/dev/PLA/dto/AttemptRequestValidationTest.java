package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.AttemptAnswerRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptSearchRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AttemptRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void attemptRequest_valid() {
        assertTrue(messages(AttemptRequest.builder()
                        .answers(List.of(
                                AttemptAnswerRequest.builder()
                                        .questionId(1L)
                                        .optionId(2L)
                                        .build(),
                                AttemptAnswerRequest.builder()
                                        .questionId(3L)
                                        .answerText("goes")
                                        .build(),
                                // Bỏ trống cả optionId lẫn answerText = bỏ qua câu, vẫn hợp lệ
                                AttemptAnswerRequest.builder().questionId(4L).build()))
                        .build())
                .isEmpty());
    }

    @Test
    void attemptRequest_rejectsNullOrEmptyAnswers() {
        assertEquals(Set.of("ATTEMPT_ANSWERS_EMPTY"), messages(new AttemptRequest()));
        assertEquals(
                Set.of("ATTEMPT_ANSWERS_EMPTY"),
                messages(AttemptRequest.builder().answers(List.of()).build()));
    }

    @Test
    void attemptRequest_validatesNestedAnswers() {
        assertEquals(
                Set.of("ATTEMPT_QUESTION_ID_BLANK", "ATTEMPT_ANSWER_TEXT_SIZE"),
                messages(AttemptRequest.builder()
                        .answers(List.of(AttemptAnswerRequest.builder()
                                .answerText("a".repeat(256))
                                .build()))
                        .build()));
    }

    @Test
    void answerText_maxLengthIs255() {
        assertTrue(messages(AttemptAnswerRequest.builder()
                        .questionId(1L)
                        .answerText("a".repeat(255))
                        .build())
                .isEmpty());
    }

    @Test
    void searchRequest_defaultsAreValid() {
        AttemptSearchRequest request = new AttemptSearchRequest();
        assertEquals(0, request.getPage());
        assertEquals(20, request.getSize());
        assertTrue(messages(request).isEmpty());
    }

    @Test
    void searchRequest_rejectsInvalidPaging() {
        assertEquals(
                Set.of("PAGE_INVALID", "PAGE_SIZE_INVALID"),
                messages(AttemptSearchRequest.builder().page(-1).size(0).build()));
        assertEquals(
                Set.of("PAGE_SIZE_INVALID"),
                messages(AttemptSearchRequest.builder().size(101).build()));
        assertTrue(messages(AttemptSearchRequest.builder().size(100).build()).isEmpty());
    }
}
