package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.OptionRequest;
import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.entities.quiz.Option;

public class OptionMapper {

    /** Gán content (trim) / isCorrect từ request. position và việc gỡ đáp án đúng cũ do service quyết định. */
    public static void updateOption(Option option, OptionRequest request) {
        option.setContent(request.getContent().trim());
        option.setCorrect(Boolean.TRUE.equals(request.getIsCorrect()));
    }

    public static OptionResponse toOptionResponse(Option option) {
        return OptionResponse.builder()
                .id(option.getId())
                .questionId(option.getQuestion().getId())
                .position(option.getPosition())
                .content(option.getContent())
                .isCorrect(option.isCorrect())
                .build();
    }
}
