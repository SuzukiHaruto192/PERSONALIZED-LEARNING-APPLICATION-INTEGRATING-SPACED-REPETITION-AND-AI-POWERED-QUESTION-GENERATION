package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.services.quiz.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topics")
@Tag(name = "Topic", description = "API CRUD topic thuộc topic group")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicController {
    TopicService topicService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách topic",
            description = "Lấy tất cả topic. Để lọc theo group, dùng GET /api/v1/topic-groups/{id}/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopics() {
        return ResponseEntity.ok(ApiResponse.<List<TopicResponse>>builder()
                .data(topicService.getTopics())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết topic", description = "Lấy topic theo id")
    public ResponseEntity<ApiResponse<TopicResponse>> getTopic(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.getTopicById(id))
                .build());
    }

    @PostMapping
    @Operation(summary = "Tạo topic", description = "Tạo topic mới thuộc một topic group")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(@Validated @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.createTopic(request))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật topic", description = "Cập nhật toàn bộ thông tin topic theo id")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(
            @PathVariable Long id, @Validated @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.updateTopic(id, request))
                .build());
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Bật/tắt topic", description = "Cập nhật trạng thái hoạt động (isActive) của topic")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopicActive(
            @PathVariable Long id, @RequestParam(defaultValue = "true") boolean active) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.updateTopicActive(id, active))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa topic", description = "Xóa topic theo id")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<Void>builder().data(topicService.deleteTopic(id)).build());
    }
}
