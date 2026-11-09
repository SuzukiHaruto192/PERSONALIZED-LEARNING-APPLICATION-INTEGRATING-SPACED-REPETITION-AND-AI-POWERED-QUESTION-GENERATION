package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SkillRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private SkillRequest.SkillRequestBuilder valid() {
        return SkillRequest.builder().name("Ngữ pháp").code("GRAMMAR");
    }

    private Set<String> invalidFields(SkillRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void requiredFieldsOnly_isValid() {
        assertTrue(invalidFields(valid().build()).isEmpty());
    }

    @Test
    void allFieldsValid_isValid() {
        assertTrue(invalidFields(valid().icon("book").build()).isEmpty());
    }

    @Test
    void nullName_isInvalid() {
        assertEquals(Set.of("name"), invalidFields(valid().name(null).build()));
    }

    @Test
    void blankName_isInvalid() {
        assertEquals(Set.of("name"), invalidFields(valid().name("   ").build()));
    }

    @Test
    void nullCode_isInvalid() {
        assertEquals(Set.of("code"), invalidFields(valid().code(null).build()));
    }

    @Test
    void blankCode_isInvalid() {
        assertEquals(Set.of("code"), invalidFields(valid().code("   ").build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"grammar", "Grammar", "GRAMMAR1", "GRAM-MAR", "_GRAMMAR", "GRAMMAR_", "GRAM__MAR", "NGỮ"})
    void malformedCode_isInvalid(String code) {
        assertEquals(Set.of("code"), invalidFields(valid().code(code).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"VOCAB", "A", "READING_PART_ONE"})
    void wellFormedCode_isValid(String code) {
        assertTrue(invalidFields(valid().code(code).build()).isEmpty());
    }

    @Test
    void fieldsAtMaxLength_areValid() {
        assertTrue(invalidFields(valid().name("a".repeat(50))
                        .code("A".repeat(20))
                        .icon("a".repeat(50))
                        .build())
                .isEmpty());
    }

    @Test
    void fieldsTooLong_areInvalid() {
        assertEquals(
                Set.of("name", "code", "icon"),
                invalidFields(valid().name("a".repeat(51))
                        .code("A".repeat(21))
                        .icon("a".repeat(51))
                        .build()));
    }
}
