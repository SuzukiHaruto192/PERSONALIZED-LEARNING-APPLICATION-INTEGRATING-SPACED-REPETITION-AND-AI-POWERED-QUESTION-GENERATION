package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.DueProgressSearchRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.DueTopicProgressResponse;
import backend.dev.PLA.mapper.TopicProgressMapper;
import backend.dev.PLA.repositories.quiz.UserTopicProgressRepository;
import backend.dev.PLA.security.CustomUserDetail;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phần đọc của user_topic_progress (phần ghi nằm ở TopicProgressService). User luôn lấy từ token nên chỉ đọc được
 * tiến độ của chính mình.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicProgressQueryService {
    UserTopicProgressRepository userTopicProgressRepository;

    /** Cặp (skill, topic) đến hạn ôn (next_review <= hôm nay), phân trang, quá hạn lâu nhất trước. */
    @Transactional(readOnly = true)
    public PageResponse<DueTopicProgressResponse> getDueProgress(
            CustomUserDetail userDetail, DueProgressSearchRequest request) {
        LocalDate today = LocalDate.now();
        return PageResponse.of(
                userTopicProgressRepository.findDue(
                        userDetail.getId(), today, PageRequest.of(request.getPage(), request.getSize())),
                progress -> TopicProgressMapper.toDueResponse(progress, today));
    }
}
