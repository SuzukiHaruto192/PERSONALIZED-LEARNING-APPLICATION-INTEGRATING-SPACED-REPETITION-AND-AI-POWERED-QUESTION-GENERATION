package backend.dev.PLA.configurations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest
class AiConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier(AiConfig.AI_EXECUTOR)
    private ThreadPoolTaskExecutor aiExecutor;

    @Test
    void chatClient_isRegistered() {
        assertThat(context.getBean(ChatClient.class)).isNotNull();
    }

    @Test
    void aiExecutor_hasTwoToFourThreads() {
        assertThat(aiExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(aiExecutor.getMaxPoolSize()).isEqualTo(4);
        assertThat(aiExecutor.getThreadNamePrefix()).isEqualTo("ai-");
    }

    @Test
    void defaultTaskExecutor_stillAutoConfigured() {
        // aiExecutor không được "chiếm" executor mặc định của Spring Boot (dùng cho @Async không tên, MVC async)
        assertThat(context.containsBean("applicationTaskExecutor")).isTrue();
    }
}
