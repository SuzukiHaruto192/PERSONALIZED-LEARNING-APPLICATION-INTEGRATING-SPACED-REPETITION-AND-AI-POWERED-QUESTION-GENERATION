package backend.dev.PLA.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.enums.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QuestionMapperTest {

    private final Quiz quiz = Quiz.builder().id(1L).title("Quiz A").build();
    private final Topic grammar =
            Topic.builder().id(40L).nameVi("Thì hiện tại đơn").build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== includeAnswers =====================

    @Nested
    class IncludeAnswers {
        @Test
        void false_hidesAnswerFields_andTheyAreAbsentFromJson() {
            Question question = singleChoice(option(2, "B", false), option(1, "A", true));

            QuestionResponse response = QuestionMapper.toQuestionResponse(question, false);

            // Phần đề vẫn đủ, options sắp theo position
            assertEquals(15L, response.getId());
            assertEquals(1L, response.getQuizId());
            assertEquals("What time does Tom get up?", response.getContent());
            assertEquals(
                    List.of("A", "B"),
                    response.getOptions().stream()
                            .map(OptionResponse::getContent)
                            .toList());

            assertNull(response.getCorrectText());
            assertNull(response.getExplanation());
            assertNull(response.getGrammarTopic());
            assertNull(response.getIsComplete());
            response.getOptions().forEach(o -> assertNull(o.getIsCorrect()));

            // Field ẩn không xuất hiện trong JSON (không phải null)
            JsonNode json = objectMapper.valueToTree(response);
            for (String field : List.of("correctText", "explanation", "grammarTopic", "isComplete")) {
                assertFalse(json.has(field), field);
            }
            json.get("options").forEach(o -> assertFalse(o.has("isCorrect")));
            assertTrue(json.has("content"));
        }

        @Test
        void true_includesAnswerFields() {
            Question question = singleChoice(option(1, "A", true), option(2, "B", false));

            QuestionResponse response = QuestionMapper.toQuestionResponse(question, true);

            assertEquals("Đoạn văn nói Tom dậy lúc 6 giờ.", response.getExplanation());
            assertEquals(40L, response.getGrammarTopic().getId());
            assertTrue(response.getIsComplete());
            assertEquals(
                    List.of(true, false),
                    response.getOptions().stream()
                            .map(OptionResponse::getIsCorrect)
                            .toList());

            JsonNode json = objectMapper.valueToTree(response);
            assertTrue(json.get("options").get(0).get("isCorrect").asBoolean());
            assertTrue(json.get("isComplete").asBoolean());
        }

        @Test
        void fillBlank_true_includesCorrectText() {
            Question question = question(QuestionType.FILL_BLANK, "goes");

            QuestionResponse response = QuestionMapper.toQuestionResponse(question, true);

            assertEquals("goes", response.getCorrectText());
            assertTrue(response.getIsComplete());
            assertNull(QuestionMapper.toQuestionResponse(question, false).getCorrectText());
        }

        @Test
        void optionMapper_false_hidesIsCorrect() {
            Option option = option(1, "A", true);
            option.setQuestion(question(QuestionType.SINGLE_CHOICE, null));

            assertNull(OptionMapper.toOptionResponse(option, false).getIsCorrect());
            assertTrue(OptionMapper.toOptionResponse(option, true).getIsCorrect());
        }
    }

    // ===================== isComplete =====================

    @Nested
    class IsComplete {
        @Test
        void singleChoice_needsAtLeastTwoOptionsAndExactlyOneCorrect() {
            List<Option> options = new ArrayList<>(List.of(option(1, "goes", true)));
            assertFalse(QuestionMapper.isComplete(QuestionType.SINGLE_CHOICE, null, options));

            options.add(option(2, "go", false));
            assertTrue(QuestionMapper.isComplete(QuestionType.SINGLE_CHOICE, null, options));

            // Xoá đáp án đúng → không còn đáp án đúng nào
            options.removeFirst();
            options.add(option(3, "went", false));
            assertFalse(QuestionMapper.isComplete(QuestionType.SINGLE_CHOICE, null, options));
        }

        @Test
        void singleChoice_moreThanSixOptions_isIncomplete() {
            List<Option> options = new ArrayList<>(List.of(option(1, "A", true)));
            for (int i = 2; i <= 7; i++) {
                options.add(option(i, "X" + i, false));
            }
            assertFalse(QuestionMapper.isComplete(QuestionType.SINGLE_CHOICE, null, options));
        }

        @Test
        void fillBlank_needsCorrectText() {
            assertTrue(QuestionMapper.isComplete(QuestionType.FILL_BLANK, "goes", List.of()));
            assertFalse(QuestionMapper.isComplete(QuestionType.FILL_BLANK, null, List.of()));
            assertFalse(QuestionMapper.isComplete(QuestionType.FILL_BLANK, "  ", List.of()));
        }
    }

    // ===================== Helpers =====================

    private Question singleChoice(Option... options) {
        Question question = question(QuestionType.SINGLE_CHOICE, null);
        question.setOptions(List.of(options));
        for (Option option : options) {
            option.setQuestion(question);
        }
        return question;
    }

    private Question question(QuestionType type, String correctText) {
        return Question.builder()
                .id(15L)
                .quiz(quiz)
                .position((short) 1)
                .type(type)
                .content("What time does Tom get up?")
                .correctText(correctText)
                .explanation("Đoạn văn nói Tom dậy lúc 6 giờ.")
                .grammarTopic(grammar)
                .options(List.of())
                .build();
    }

    private static Option option(int position, String content, boolean correct) {
        return Option.builder()
                .id(30L + position)
                .position((short) position)
                .content(content)
                .isCorrect(correct)
                .build();
    }
}
