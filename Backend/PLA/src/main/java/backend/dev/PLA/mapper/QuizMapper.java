package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.QuizRequest;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.dto.response.quiz.RefResponse;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizFormat;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;

public class QuizMapper {

    /** Gán thông tin chung từ request. skill/topic/format do service đã kiểm tra và truyền vào. */
    public static void updateQuiz(Quiz quiz, QuizRequest request, Skill skill, Topic topic, QuizFormat format) {
        quiz.setTitle(request.getTitle().trim());
        quiz.setDescription(trimToNull(request.getDescription()));
        quiz.setSkill(skill);
        quiz.setTopic(topic);
        quiz.setCustomTopic(topic == null ? trimToNull(request.getCustomTopic()) : null);
        quiz.setFormat(format);
        quiz.setDifficulty(request.getDifficulty());
    }

    public static QuizResponse toQuizResponse(Quiz quiz, long questionCount) {
        Skill skill = quiz.getSkill();
        Topic topic = quiz.getTopic();
        QuizFormat format = quiz.getFormat();
        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .skill(skill == null ? null : new RefResponse(skill.getId().longValue(), skill.getName()))
                .topic(topic == null ? null : new RefResponse(topic.getId(), topic.getNameVi()))
                .customTopic(quiz.getCustomTopic())
                .format(format == null ? null : new RefResponse(format.getId().longValue(), format.getName()))
                .difficulty(quiz.getDifficulty())
                .status(quiz.getStatus())
                .createdBy(
                        quiz.getCreatedBy() == null ? null : quiz.getCreatedBy().getId())
                .createdAt(quiz.getCreatedAt())
                .questionCount(questionCount)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
