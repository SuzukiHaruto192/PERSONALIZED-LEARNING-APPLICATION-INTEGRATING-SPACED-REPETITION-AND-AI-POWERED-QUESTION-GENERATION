package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.quiz.AttemptAnswerResponse;
import backend.dev.PLA.dto.response.quiz.AttemptQuestionResponse;
import backend.dev.PLA.dto.response.quiz.AttemptResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.dto.response.quiz.RefResponse;
import backend.dev.PLA.entities.quiz.AttemptAnswer;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizAttempt;
import backend.dev.PLA.entities.quiz.Skill;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AttemptMapper {

    /** Thông tin tổng quát của lượt làm (không có questions). quiz/skill nên được fetch sẵn. */
    public static AttemptResponse toAttemptResponse(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Skill skill = quiz.getSkill();
        return AttemptResponse.builder()
                .id(attempt.getId())
                .quiz(new RefResponse(quiz.getId(), quiz.getTitle()))
                .skill(skill == null ? null : new RefResponse(skill.getId().longValue(), skill.getName()))
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .correctCount(attempt.getCorrectCount())
                .totalQuestions(attempt.getTotalQuestions())
                .build();
    }

    /**
     * Response lịch sử: ghép từng câu hỏi (đã sắp theo position, kèm options) với câu trả lời của lượt làm theo
     * questionId. Câu không có câu trả lời thì answer = null.
     */
    public static AttemptResponse toAttemptHistoryResponse(
            QuizAttempt attempt, List<QuestionResponse> questions, List<AttemptAnswer> answers) {
        Map<Long, AttemptAnswerResponse> answerByQuestion = new HashMap<>();
        for (AttemptAnswer answer : answers) {
            if (!Objects.equals(answer.getId().getAttemptId(), attempt.getId())) {
                continue;
            }
            answerByQuestion.put(answer.getId().getQuestionId(), toAttemptAnswerResponse(answer));
        }
        AttemptResponse response = toAttemptResponse(attempt);
        response.setQuestions(questions.stream()
                .map(question -> new AttemptQuestionResponse(question, answerByQuestion.get(question.getId())))
                .toList());
        return response;
    }

    public static AttemptAnswerResponse toAttemptAnswerResponse(AttemptAnswer answer) {
        return AttemptAnswerResponse.builder()
                .attemptId(answer.getId().getAttemptId())
                .questionId(answer.getId().getQuestionId())
                .selectedOptionId(
                        answer.getSelectedOption() == null
                                ? null
                                : answer.getSelectedOption().getId())
                .answerText(answer.getAnswerText())
                .isCorrect(answer.isCorrect())
                .build();
    }
}
