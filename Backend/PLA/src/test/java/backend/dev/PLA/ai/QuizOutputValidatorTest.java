package backend.dev.PLA.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.dev.PLA.dto.ai.GeneratedQuiz;
import backend.dev.PLA.exceptions.AiOutputInvalidException;
import backend.dev.PLA.services.ai.QuizOutputValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

class QuizOutputValidatorTest {
    private static final int QUESTION_COUNT = 20;

    QuizOutputValidator validator = new QuizOutputValidator(new BeanOutputConverter<>(GeneratedQuiz.class));

    private static final String SINGLE_OK = """
            {"type":"SINGLE_CHOICE","content":"He ___ to school.","options":[
              {"content":"go","isCorrect":false},{"content":"goes","isCorrect":true},
              {"content":"going","isCorrect":false}]}""";
    private static final String FILL_OK = """
            {"type":"FILL_BLANK","content":"She ___ tea.","correctText":"likes","options":[]}""";

    private static String passage(List<String> questions) {
        return "{\"content\":\"Tom gets up at 6.\",\"questions\":[" + String.join(",", questions) + "]}";
    }

    /** Quiz gồm các passage cho trước. */
    private static String quiz(String... passages) {
        return "{\"passages\":[" + String.join(",", passages) + "]}";
    }

    private static List<String> repeat(String q, int n) {
        return IntStream.range(0, n).mapToObj(i -> q).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> errorsOf(String json) {
        try {
            validator.validate(json, QUESTION_COUNT);
        } catch (AiOutputInvalidException e) {
            return e.getErrors();
        }
        throw new AssertionError("Phải ném AiOutputInvalidException");
    }

    @Test
    void validQuiz_returnsParsedObject() {
        GeneratedQuiz quiz = validator.validate(quiz(passage(repeat(SINGLE_OK, QUESTION_COUNT))), QUESTION_COUNT);
        assertThat(quiz.getPassages().get(0).getQuestions()).hasSize(QUESTION_COUNT);
        assertThat(quiz.getPassages().get(0).getQuestions().get(0).getOptions()).hasSize(3);
    }

    @Test
    void questionCountFollowsRequestedCount() {
        assertThat(validator
                        .validate(quiz(passage(repeat(SINGLE_OK, 40))), 40)
                        .getPassages()
                        .get(0)
                        .getQuestions())
                .hasSize(40);
        assertThatThrownBy(() -> validator.validate(quiz(passage(repeat(SINGLE_OK, 20))), 40))
                .isInstanceOf(AiOutputInvalidException.class);
    }

    @Test
    void questionCountSummedAcrossPassages() {
        GeneratedQuiz quiz = validator.validate(
                quiz(passage(repeat(SINGLE_OK, QUESTION_COUNT - 1)), passage(repeat(FILL_OK, 1))), QUESTION_COUNT);
        assertThat(quiz.getPassages()).hasSize(2);
    }

    @Test
    void notJson_throwsWithParseError() {
        assertThatThrownBy(() -> validator.validate("hello", QUESTION_COUNT))
                .isInstanceOf(AiOutputInvalidException.class)
                .hasMessageContaining("Không parse được JSON");
    }

    @Test
    void blankContent_throws() {
        assertThat(errorsOf("  ")).anyMatch(e -> e.contains("rỗng"));
    }

    @Test
    void noPassage_throws() {
        assertThat(errorsOf(quiz())).anyMatch(e -> e.contains("passages phải có ít nhất 1 đoạn văn"));
    }

    @Test
    void passageWithoutQuestions_throws() {
        assertThat(errorsOf(quiz(passage(repeat(FILL_OK, QUESTION_COUNT)), passage(List.of()))))
                .containsExactly("passages[1] phải có ít nhất 1 câu hỏi.");
    }

    @Test
    void wrongQuestionCount_throws() {
        assertThat(errorsOf(quiz(passage(repeat(FILL_OK, 5)))))
                .containsExactly("Tổng số câu hỏi là 5, yêu cầu đúng " + QUESTION_COUNT + ".");
    }

    @Test
    void singleChoiceWithTwoCorrectAnswers_throws() {
        String bad = """
                {"type":"SINGLE_CHOICE","content":"Q","options":[
                  {"content":"a","isCorrect":true},{"content":"b","isCorrect":true},
                  {"content":"c","isCorrect":false}]}""";
        List<String> questions = repeat(FILL_OK, QUESTION_COUNT - 1);
        questions.add(bad);
        assertThat(errorsOf(quiz(passage(questions))))
                .containsExactly("passages[0].questions[" + (QUESTION_COUNT - 1)
                        + "]: phải có đúng 1 đáp án isCorrect=true, đang có 2.");
    }

    @Test
    void singleChoiceTooFewOptionsAndDuplicate_throws() {
        String bad = """
                {"type":"SINGLE_CHOICE","content":"Q","options":[
                  {"content":"Go","isCorrect":true},{"content":"go ","isCorrect":false}]}""";
        List<String> questions = repeat(SINGLE_OK, QUESTION_COUNT - 1);
        questions.add(bad);
        assertThat(errorsOf(quiz(passage(questions))))
                .anyMatch(e -> e.contains("phải có 3-4 đáp án"))
                .anyMatch(e -> e.contains("trùng nội dung"));
    }

    @Test
    void fillBlankWithoutCorrectText_throws() {
        List<String> questions = repeat(FILL_OK, QUESTION_COUNT - 1);
        questions.add("{\"type\":\"FILL_BLANK\",\"content\":\"Q\",\"options\":[]}");
        assertThat(errorsOf(quiz(passage(questions))))
                .containsExactly(
                        "passages[0].questions[" + (QUESTION_COUNT - 1) + "]: FILL_BLANK phải có correctText.");
    }
}
