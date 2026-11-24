package backend.dev.PLA.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.genai.Client;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

/**
 * Gọi Gemini THẬT để kiểm tra GOOGLE_API_KEY trong .env (hoặc biến môi trường) còn dùng được.
 *
 * Không chạy cùng `mvnw test` thông thường (bị loại qua tag "live-ai" trong surefire) để
 * không tốn quota và không fail khi offline. Chạy riêng:
 *
 *   ./mvnw test -Dtest=GeminiApiKeyLiveTest -DexcludedTestGroups=none
 *
 * Không cần Spring context hay database: dựng ChatModel trực tiếp từ key.
 */
@Tag("live-ai")
class GeminiApiKeyLiveTest {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    @Test
    void apiKey_isValid_geminiReturnsAnswer() throws IOException {
        Map<String, String> env = loadEnv();
        String apiKey = env.get("GOOGLE_API_KEY");
        assumeTrue(
                apiKey != null && !apiKey.isBlank(),
                "GOOGLE_API_KEY chưa được đặt trong .env hoặc biến môi trường -> bỏ qua");
        String model = env.getOrDefault("GEMINI_MODEL", DEFAULT_MODEL);

        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(Client.builder().apiKey(apiKey).build())
                .options(GoogleGenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.0)
                        .build())
                .build();

        // Key sai/hết hạn -> Gemini trả 400/403 và call() ném exception -> test fail kèm lý do
        ChatResponse response = chatModel.call(new Prompt("Reply with exactly one word: OK"));

        String text = response.getResult().getOutput().getText();
        assertThat(text).as("Gemini (%s) phải trả về nội dung", model).isNotBlank();
        assertThat(text.toUpperCase()).contains("OK");
    }

    /** Biến môi trường được ưu tiên hơn file .env (giống thứ tự Spring dùng khi chạy app). */
    private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>();
        Path dotEnv = Path.of(".env");
        if (Files.exists(dotEnv)) {
            for (String line : Files.readAllLines(dotEnv)) {
                String trimmed = line.trim();
                int eq = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || eq <= 0) {
                    continue;
                }
                values.put(
                        trimmed.substring(0, eq).trim(),
                        trimmed.substring(eq + 1).trim());
            }
        }
        for (String key : new String[] {"GOOGLE_API_KEY", "GEMINI_MODEL"}) {
            String fromEnv = System.getenv(key);
            if (fromEnv != null && !fromEnv.isBlank()) {
                values.put(key, fromEnv);
            }
        }
        return values;
    }
}
