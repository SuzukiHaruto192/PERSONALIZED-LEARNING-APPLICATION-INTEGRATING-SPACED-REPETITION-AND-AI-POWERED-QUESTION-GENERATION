package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.enums.TopicGroupType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UpdateTopicGroupRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(UpdateTopicGroupRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void allFieldsNull_isValid() {
        assertTrue(invalidFields(UpdateTopicGroupRequest.builder().build()).isEmpty());
    }

    @Test
    void allFieldsValid_isValid() {
        assertTrue(invalidFields(UpdateTopicGroupRequest.builder()
                        .type(TopicGroupType.GRAMMAR)
                        .name("  Thì hiện tại ")
                        .icon("clock")
                        .sortOrder(0)
                        .build())
                .isEmpty());
    }

    @Test
    void emptyName_isInvalid() {
        assertEquals(
                Set.of("name"),
                invalidFields(UpdateTopicGroupRequest.builder().name("").build()));
    }

    @Test
    void whitespaceOnlyName_isInvalid() {
        assertEquals(
                Set.of("name"),
                invalidFields(UpdateTopicGroupRequest.builder().name("   ").build()));
    }

    @Test
    void nameTooLong_isInvalid() {
        assertEquals(
                Set.of("name"),
                invalidFields(
                        UpdateTopicGroupRequest.builder().name("a".repeat(101)).build()));
    }

    @Test
    void iconTooLong_isInvalid() {
        assertEquals(
                Set.of("icon"),
                invalidFields(
                        UpdateTopicGroupRequest.builder().icon("a".repeat(51)).build()));
    }

    @Test
    void negativeSortOrder_isInvalid() {
        assertEquals(
                Set.of("sortOrder"),
                invalidFields(UpdateTopicGroupRequest.builder().sortOrder(-1).build()));
    }
}
