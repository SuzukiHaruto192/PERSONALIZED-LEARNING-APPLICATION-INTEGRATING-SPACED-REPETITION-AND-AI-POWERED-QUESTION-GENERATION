package backend.dev.PLA.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Cấu trúc JSON mà LLM phải trả về: quiz → passages → questions → options. BeanOutputConverter sinh JSON schema từ
 * class này và gắn vào prompt, nên description của từng field cũng là chỉ dẫn cho LLM.
 *
 * <p>Không dùng QuizResponse làm schema: QuizResponse là dạng dẹt (passages và questions tách rời, nối bằng passageId)
 * và có các field server tự gán (id, position, status...). LLM chỉ cần sinh nội dung theo cấu trúc lồng nhau.
 * Không có title: title do người dùng nhập (QuizRequest.title).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedQuiz {
    @JsonPropertyDescription("Mô tả ngắn bằng tiếng Việt")
    String description;

    @JsonPropertyDescription("Danh sách đoạn văn, ít nhất 1. Mọi câu hỏi đều nằm trong passages[].questions")
    List<GeneratedPassage> passages;
}
