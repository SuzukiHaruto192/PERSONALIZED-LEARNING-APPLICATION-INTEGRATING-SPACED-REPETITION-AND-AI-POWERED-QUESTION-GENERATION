package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.services.quiz.TopicGroupService;
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
@RequestMapping("/api/v1/topic-groups")
@Tag(name = "Topic Group", description = "API topic group")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicGroupController {
    TopicGroupService topicGroupService;
    TopicService topicService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách topic group",
            description = "ấy danh sách topic group . ví dụ : thì hiện tại , du lịch ,... ")
    public ResponseEntity<ApiResponse<List<TopicGroupResponse>>> getTopicGroups() {
        return ResponseEntity.ok(ApiResponse.<List<TopicGroupResponse>>builder()
                .data(topicGroupService.getTopicGroup())
                .build());
    }

    @GetMapping("/{id}/topics")
    @Operation(
            summary = "Lấy danh sách topic theo topic group",
            description =
                    "Lấy các topic thuộc topic group theo id, sắp xếp theo sortOrder. Trả 404 nếu group không tồn tại")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopicsByGroup(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<List<TopicResponse>>builder()
                .data(topicService.getTopicsByGroup(id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "tạo danh sách topic group",
            description = "Tạo danh sách topic group . ví dụ : thì hiện tại , du lịch ,... ")
    public ResponseEntity<ApiResponse<TopicGroupResponse>> getTopicGroup(
            @Validated @RequestBody TopicGroupRequest topicGroupRequest) {
        return ResponseEntity.ok(ApiResponse.<TopicGroupResponse>builder()
                .data(topicGroupService.createTopicGroup(topicGroupRequest))
                .build());
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Cập nhật topic group",
            description = "Cập nhật một phần topic group theo id: chỉ gửi trường cần đổi, trường null giữ nguyên. "
                    + "Trả 404 nếu không tồn tại, 400 nếu trùng name + type")
    public ResponseEntity<ApiResponse<TopicGroupResponse>> updateTopicGroup(
            @PathVariable Long id, @Validated @RequestBody UpdateTopicGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.<TopicGroupResponse>builder()
                .data(topicGroupService.updateTopicGroup(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = " xóa topic group", description = "xóa topic group theo id ")
    public ResponseEntity<ApiResponse<Void>> deleteTopicGroup(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .data(topicGroupService.DeleteTopicGroup(id))
                .build());
    }
}
