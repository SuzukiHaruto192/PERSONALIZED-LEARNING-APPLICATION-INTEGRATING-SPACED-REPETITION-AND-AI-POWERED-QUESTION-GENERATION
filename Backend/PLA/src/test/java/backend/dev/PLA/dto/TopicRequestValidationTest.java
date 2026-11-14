package backend.dev.PLA.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.enums.CefrLevel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TopicRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private TopicRequest.TopicRequestBuilder valid() {
        return TopicRequest.builder()
                .idTopicGroup(5L)
                .slug("daily-life")
                .nameVi("Cuộc sống hằng ngày")
                .level(CefrLevel.A1)
                .skillIds(Set.of(1L));
    }

    private Set<String> invalidFields(TopicRequest request) {
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
        assertTrue(invalidFields(valid().nameEn("Daily life")
                        .keyword("home, family")
                        .icon("home")
                        .description("mô tả")
                        .sortBy(0)
                        .build())
                .isEmpty());
    }

    @Test
    void missingIdTopicGroup_isInvalid() {
        assertEquals(
                Set.of("idTopicGroup"), invalidFields(valid().idTopicGroup(null).build()));
    }

    @Test
    void missingLevel_isInvalid() {
        assertEquals(Set.of("level"), invalidFields(valid().level(null).build()));
    }

    @Test
    void blankNameVi_isInvalid() {
        assertEquals(Set.of("nameVi"), invalidFields(valid().nameVi("   ").build()));
    }

    @Test
    void nullSlug_isInvalid() {
        assertEquals(Set.of("slug"), invalidFields(valid().slug(null).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Daily-Life", "daily life", "daily_life", "-daily", "daily-", "daily--life", "đi-chợ"})
    void malformedSlug_isInvalid(String slug) {
        assertEquals(Set.of("slug"), invalidFields(valid().slug(slug).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"airport", "a1", "daily-life-2"})
    void wellFormedSlug_isValid(String slug) {
        assertTrue(invalidFields(valid().slug(slug).build()).isEmpty());
    }

    @Test
    void slugTooLong_isInvalid() {
        assertEquals(Set.of("slug"), invalidFields(valid().slug("a".repeat(101)).build()));
    }

    @Test
    void fieldsTooLong_areInvalid() {
        assertEquals(
                Set.of("nameVi", "nameEn", "keyword", "icon", "description"),
                invalidFields(valid().nameVi("a".repeat(101))
                        .nameEn("a".repeat(101))
                        .keyword("a".repeat(256))
                        .icon("a".repeat(51))
                        .description("a".repeat(2001))
                        .build()));
    }

    @Test
    void negativeSortBy_isInvalid() {
        assertEquals(Set.of("sortBy"), invalidFields(valid().sortBy(-1).build()));
    }

    @Test
    void nullSkillIds_isInvalid() {
        assertEquals(Set.of("skillIds"), invalidFields(valid().skillIds(null).build()));
    }

    @Test
    void emptySkillIds_isInvalid() {
        assertEquals(
                Set.of("skillIds"), invalidFields(valid().skillIds(Set.of()).build()));
    }

    @Test
    void nullSkillIdElement_isInvalid() {
        Set<Long> ids = new HashSet<>();
        ids.add(1L);
        ids.add(null);
        Set<String> fields = invalidFields(valid().skillIds(ids).build());
        assertEquals(1, fields.size());
        assertTrue(fields.iterator().next().startsWith("skillIds"));
    }
}
