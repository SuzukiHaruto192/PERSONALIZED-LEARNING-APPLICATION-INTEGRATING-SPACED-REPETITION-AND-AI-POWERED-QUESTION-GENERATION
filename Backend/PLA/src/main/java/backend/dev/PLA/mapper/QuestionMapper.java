package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.QuestionRequest;
import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.dto.response.quiz.RefResponse;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Passage;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.enums.QuestionType;
import java.util.Comparator;
import java.util.List;

public class QuestionMapper {
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;

    /**
     * Gán thông tin câu hỏi từ request (trim, chuỗi rỗng thành null). passage / grammarTopic do service đã kiểm tra
     * và truyền vào; position do service quyết định.
     */
    public static void updateQuestion(Question question, QuestionRequest request, Passage passage, Topic grammarTopic) {
        question.setPassage(passage);
        question.setType(request.getType());
        question.setContent(request.getContent().trim());
        question.setCorrectText(trimToNull(request.getCorrectText()));
        question.setExplanation(trimToNull(request.getExplanation()));
        question.setGrammarTopic(grammarTopic);
    }

    /** options lấy từ question.getOptions(): caller nên fetch sẵn (fetch join) để tránh N+1. */
    public static QuestionResponse toQuestionResponse(Question question) {
        List<Option> options = question.getOptions() == null ? List.of() : question.getOptions();
        List<OptionResponse> optionResponses = options.stream()
                .sorted(Comparator.comparing(Option::getPosition))
                .map(OptionMapper::toOptionResponse)
                .toList();
        Topic topic = question.getGrammarTopic();
        return QuestionResponse.builder()
                .id(question.getId())
                .quizId(question.getQuiz().getId())
                .passageId(
                        question.getPassage() == null
                                ? null
                                : question.getPassage().getId())
                .position(question.getPosition())
                .type(question.getType())
                .content(question.getContent())
                .correctText(question.getCorrectText())
                .explanation(question.getExplanation())
                .grammarTopic(topic == null ? null : new RefResponse(topic.getId(), topic.getNameVi()))
                .options(optionResponses)
                .isComplete(isComplete(question.getType(), question.getCorrectText(), options))
                .build();
    }

    // SINGLE_CHOICE: 2-6 đáp án và đúng 1 đáp án đúng; FILL_BLANK: có correctText
    static boolean isComplete(QuestionType type, String correctText, List<Option> options) {
        if (type == QuestionType.FILL_BLANK) {
            return correctText != null && !correctText.isBlank();
        }
        long correctCount = options.stream().filter(Option::isCorrect).count();
        return options.size() >= MIN_OPTIONS && options.size() <= MAX_OPTIONS && correctCount == 1;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
