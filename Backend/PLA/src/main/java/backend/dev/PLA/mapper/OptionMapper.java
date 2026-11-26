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

    /**
     * includeAnswers = false (API lấy đề): không có isCorrect. Chỉ truyền true cho response nộp bài, lịch sử lượt
     * làm và luồng nội bộ không trả ra API. Không có bản thiếu tham số để mọi chỗ gọi phải chọn rõ.
     */
    public static OptionResponse toOptionResponse(Option option, boolean includeAnswers) {
        return OptionResponse.builder()
                .id(option.getId())
                .questionId(option.getQuestion().getId())
                .position(option.getPosition())
                .content(option.getContent())
                .isCorrect(includeAnswers ? option.isCorrect() : null)
                .build();
    }
}
