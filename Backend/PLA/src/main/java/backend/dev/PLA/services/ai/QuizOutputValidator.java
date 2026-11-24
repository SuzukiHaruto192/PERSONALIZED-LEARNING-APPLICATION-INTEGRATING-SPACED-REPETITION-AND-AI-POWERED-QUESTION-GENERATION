package backend.dev.PLA.services.ai;

import static backend.dev.PLA.services.ai.AiQuizRules.*;

import backend.dev.PLA.dto.ai.GeneratedOption;
import backend.dev.PLA.dto.ai.GeneratedPassage;
import backend.dev.PLA.dto.ai.GeneratedQuestion;
import backend.dev.PLA.dto.ai.GeneratedQuiz;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.exceptions.AiOutputInvalidException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * Kiểm tra câu trả lời của LLM: parse JSON về GeneratedQuiz rồi kiểm tra ràng buộc nghiệp vụ theo cấu trúc quiz →
 * passages → questions → options.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizOutputValidator {
    // Bean trong AiConfig, dùng chung với QuizPromptService để schema trong prompt và lúc parse là một
    BeanOutputConverter<GeneratedQuiz> outputConverter;

    /**
     * Parse và kiểm tra câu trả lời.
     *
     * @param expectedQuestionCount tổng số câu hỏi người dùng yêu cầu (QuizRequest.questionCount)
     * @return quiz hợp lệ
     * @throws AiOutputInvalidException kèm toàn bộ lỗi tìm được, để gửi lại cho LLM sửa
     */
    public GeneratedQuiz validate(String content, int expectedQuestionCount) {
        List<String> errors = new ArrayList<>();
        GeneratedQuiz quiz = parse(content, errors);
        if (quiz != null) {
            validateQuiz(quiz, expectedQuestionCount, errors);
        }
        if (!errors.isEmpty()) {
            throw new AiOutputInvalidException(errors);
        }
        return quiz;
    }

    private GeneratedQuiz parse(String content, List<String> errors) {
        if (isBlank(content)) {
            errors.add("Câu trả lời rỗng, phải là một JSON object.");
            return null;
        }
        try {
            GeneratedQuiz quiz = outputConverter.convert(content);
            if (quiz == null) errors.add("JSON rỗng (null).");
            return quiz;
        } catch (RuntimeException e) {
            errors.add("Không parse được JSON: " + e.getMessage());
            return null;
        }
    }

    private void validateQuiz(GeneratedQuiz quiz, int expectedQuestionCount, List<String> errors) {
        List<GeneratedPassage> passages = nullToEmpty(quiz.getPassages());
        if (passages.isEmpty()) {
            errors.add("passages phải có ít nhất 1 đoạn văn.");
        }
        int total = 0;
        for (int i = 0; i < passages.size(); i++) {
            total += validatePassage(passages.get(i), "passages[" + i + "]", errors);
        }
        if (total != expectedQuestionCount) {
            errors.add("Tổng số câu hỏi là " + total + ", yêu cầu đúng " + expectedQuestionCount + ".");
        }
    }

    /** @return số câu hỏi trong passage */
    private int validatePassage(GeneratedPassage passage, String path, List<String> errors) {
        if (passage == null) {
            errors.add(path + " là null.");
            return 0;
        }
        if (isBlank(passage.getContent())) errors.add(path + ".content không được rỗng.");
        List<GeneratedQuestion> questions = nullToEmpty(passage.getQuestions());
        if (questions.isEmpty()) errors.add(path + " phải có ít nhất 1 câu hỏi.");
        for (int j = 0; j < questions.size(); j++) {
            validateQuestion(questions.get(j), path + ".questions[" + j + "]", errors);
        }
        return questions.size();
    }

    private void validateQuestion(GeneratedQuestion q, String path, List<String> errors) {
        if (q == null) {
            errors.add(path + " là null.");
            return;
        }
        if (isBlank(q.getContent())) errors.add(path + ".content không được rỗng.");
        if (q.getType() == null) {
            errors.add(path + ".type phải là SINGLE_CHOICE hoặc FILL_BLANK.");
            return;
        }
        if (q.getType() == QuestionType.FILL_BLANK) {
            validateFillBlank(q, path, errors);
        } else {
            validateSingleChoice(q, path, errors);
        }
    }

    private void validateFillBlank(GeneratedQuestion q, String path, List<String> errors) {
        if (isBlank(q.getCorrectText())) {
            errors.add(path + ": FILL_BLANK phải có correctText.");
        } else if (q.getCorrectText().trim().length() > CORRECT_TEXT_MAX_LENGTH) {
            errors.add(path + ".correctText vượt quá " + CORRECT_TEXT_MAX_LENGTH + " ký tự.");
        }
        if (!nullToEmpty(q.getOptions()).isEmpty()) errors.add(path + ": FILL_BLANK phải có options=[].");
    }

    private void validateSingleChoice(GeneratedQuestion q, String path, List<String> errors) {
        List<GeneratedOption> options = nullToEmpty(q.getOptions());
        if (!isBlank(q.getCorrectText())) errors.add(path + ": SINGLE_CHOICE phải để correctText=null.");
        if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            errors.add(path + ": SINGLE_CHOICE phải có " + MIN_OPTIONS + "-" + MAX_OPTIONS + " đáp án, đang có "
                    + options.size() + ".");
        }
        int correctCount = 0;
        Set<String> seen = new HashSet<>();
        for (int k = 0; k < options.size(); k++) {
            GeneratedOption option = options.get(k);
            String optionPath = path + ".options[" + k + "]";
            if (option == null || isBlank(option.getContent())) {
                errors.add(optionPath + ".content không được rỗng.");
                continue;
            }
            if (!seen.add(option.getContent().trim().toLowerCase())) {
                errors.add(optionPath + " trùng nội dung với đáp án khác.");
            }
            if (Boolean.TRUE.equals(option.getIsCorrect())) correctCount++;
        }
        if (correctCount != 1) {
            errors.add(path + ": phải có đúng 1 đáp án isCorrect=true, đang có " + correctCount + ".");
        }
    }
}
