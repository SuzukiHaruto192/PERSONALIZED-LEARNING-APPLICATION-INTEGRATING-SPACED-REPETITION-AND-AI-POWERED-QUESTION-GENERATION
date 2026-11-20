package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.DueProgressSearchRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.DueTopicProgressResponse;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.TopicProgressQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/progress")
@Tag(name = "Topic progress", description = "API tiến độ học và lịch ôn FSRS theo cặp (skill, topic) của user hiện tại")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicProgressController {
    TopicProgressQueryService topicProgressQueryService;

    @GetMapping("/due")
    @Operation(
            summary = "Chủ đề đến hạn ôn",
            description = "Các cặp (skill, topic) có next_review <= hôm nay, phân trang, quá hạn lâu nhất trước; cùng "
                    + "ngày thì mastery thấp trước. Mỗi phần tử gồm skill, topic và overdueDays (số ngày quá hạn, 0 = "
                    + "đến hạn hôm nay). Chỉ gồm chủ đề đã làm quiz ít nhất 1 lần. Danh sách rỗng vẫn trả 200. Lỗi: "
                    + "1006 với errors.page = PAGE_INVALID / errors.size = PAGE_SIZE_INVALID.")
    public ResponseEntity<ApiResponse<PageResponse<DueTopicProgressResponse>>> getDueProgress(
            @AuthenticationPrincipal CustomUserDetail userDetail,
            @ParameterObject @Validated @ModelAttribute DueProgressSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(topicProgressQueryService.getDueProgress(userDetail, request)));
    }
}
