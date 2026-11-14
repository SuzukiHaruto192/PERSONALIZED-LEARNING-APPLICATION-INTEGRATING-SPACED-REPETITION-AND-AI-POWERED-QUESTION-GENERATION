package backend.dev.PLA.controllers.quiz;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import backend.dev.PLA.dto.response.ApiResponse;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.services.quiz.SkillService;
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
@RequestMapping("/api/v1/skills")
@Tag(name = "Skill", description = "API skill")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillController {
    SkillService skillService;

    @GetMapping
    @Operation(summary = "Lấy danh sách skill", description = "Lấy tất cả skill. ví dụ : GRAMMAR, VOCAB, ...")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills() {
        return ResponseEntity.ok(ApiResponse.<List<SkillResponse>>builder()
                .data(skillService.getSkills())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy skill và các topic group con và các topic con của topic group theo id",
            description = "Trả 404 nếu skill không tồn tại")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<SkillResponse>builder()
                .data(skillService.getSkillById(id))
                .build());
    }

    @PostMapping
    @Operation(summary = "Tạo skill", description = "Tạo skill mới. Trả 400 nếu code đã tồn tại")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Validated @RequestBody SkillRequest request) {
        return ResponseEntity.ok(ApiResponse.<SkillResponse>builder()
                .data(skillService.saveSkill(request))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật skill",
            description = "Cập nhật toàn bộ skill theo id. Trả 404 nếu không tồn tại, 400 nếu trùng code")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable Long id, @Validated @RequestBody SkillRequest request) {
        return ResponseEntity.ok(ApiResponse.<SkillResponse>builder()
                .data(skillService.updateSkill(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa skill", description = "Xóa skill theo id. Trả 400 nếu skill đang được sử dụng")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<Void>builder().data(skillService.deleteSkill(id)).build());
    }
}
