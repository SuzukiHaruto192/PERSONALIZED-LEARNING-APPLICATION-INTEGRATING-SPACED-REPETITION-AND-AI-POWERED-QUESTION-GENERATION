package backend.dev.PLA.controllers;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.services.quiz.TopicGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topics")
@Tag(name = "Topic", description = "API Lấy topic group")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicGroupController {
    TopicGroupService topicGroupService;


    @GetMapping()
    @Operation(
            summary = "Lấy danh sách topic group",
            description = "ấy danh sách topic group . ví dụ : thì hiện tại , du lịch ,... "
    )
    public ResponseEntity<ApiResponse<List<TopicGroupResponse>>> getTopicGroups() {
        return  ResponseEntity.ok( ApiResponse.<List<TopicGroupResponse>>builder()
                .data(topicGroupService.getTopicGroup())
                .build());
    }

    @PostMapping()
    @Operation(
            summary = "tạo danh sách topic group",
            description = "Tạo danh sách topic group . ví dụ : thì hiện tại , du lịch ,... "
    )
    public ResponseEntity<ApiResponse<TopicGroupResponse>> getTopicGroup(TopicGroupRequest topicGroupRequest) {
        return ResponseEntity.ok(
                ApiResponse.<TopicGroupResponse>builder()
                .data(topicGroupService.createTopicGroup(topicGroupRequest))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = " xóa topic group",
            description = "xóa topic group theo id "
    )
    public ResponseEntity<ApiResponse<Void>> deleteTopicGroup(@Parameter Long id) {
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(topicGroupService.DeleteTopicGroup(id))
                        .build());
    }



}
