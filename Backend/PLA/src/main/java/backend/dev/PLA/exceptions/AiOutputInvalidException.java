package backend.dev.PLA.exceptions;

import java.util.List;
import lombok.Getter;

/**
 * Câu trả lời của LLM không đạt chuẩn (sai JSON hoặc sai ràng buộc nghiệp vụ). Chỉ dùng nội bộ trong luồng sinh quiz:
 * AIGenerativeService bắt lỗi này để gửi danh sách lỗi cho LLM sửa; hết lượt thử mới đổi thành
 * AppException(AI_GENERATION_FAILED), nên lỗi này không bao giờ ra tới client.
 */
@Getter
public class AiOutputInvalidException extends RuntimeException {
    private final List<String> errors;

    public AiOutputInvalidException(List<String> errors) {
        super("LLM trả quiz sai định dạng: " + errors);
        this.errors = List.copyOf(errors);
    }
}
