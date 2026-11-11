package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.PassageRequest;
import backend.dev.PLA.dto.response.quiz.PassageResponse;
import backend.dev.PLA.entities.quiz.Passage;

public class PassageMapper {

    /** Gán content / audioUrl từ request (trim, chuỗi rỗng thành null). position do service quyết định. */
    public static void updatePassage(Passage passage, PassageRequest request) {
        passage.setContent(trimToNull(request.getContent()));
        passage.setAudioUrl(trimToNull(request.getAudioUrl()));
    }

    public static PassageResponse toPassageResponse(Passage passage, long questionCount) {
        return PassageResponse.builder()
                .id(passage.getId())
                .quizId(passage.getQuiz().getId())
                .position(passage.getPosition())
                .content(passage.getContent())
                .audioUrl(passage.getAudioUrl())
                .questionCount(questionCount)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
