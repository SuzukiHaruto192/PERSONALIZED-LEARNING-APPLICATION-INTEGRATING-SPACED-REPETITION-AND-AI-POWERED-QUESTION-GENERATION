package backend.dev.PLA.controller;

import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra các exception chuẩn của Spring MVC được GlobalExceptionHandler
 * trả về đúng status + bọc trong ApiResponse (thay vì 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicService topicService;

    private CustomUserDetail customUserDetail;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(User.builder()
                .id(1L)
                .username("test")
                .password("123456")
                .build());
    }

    // ===== 400 - sai kiểu tham số =====

    @Test
    void pathVariableTypeMismatch_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/topics/abc").with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.id").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/topics/abc"));

        Mockito.verify(topicService, Mockito.never()).getTopicById(anyLong());
    }

    // ===== 400 - body không đọc được =====

    @Test
    void malformedJson_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST_BODY.getCode()));

        Mockito.verify(topicService, Mockito.never()).createTopic(any());
    }

    @Test
    void invalidEnumValue_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTopicGroup\":5,\"slug\":\"airport\",\"nameVi\":\"Sân bay\",\"level\":\"Z1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST_BODY.getCode()));

        Mockito.verify(topicService, Mockito.never()).createTopic(any());
    }

    @Test
    void missingBody_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST_BODY.getCode()));
    }

    // ===== 405 / 415 / 404 =====

    @Test
    void methodNotSupported_methodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/v1/topics").with(user(customUserDetail)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()));
    }

    @Test
    void unsupportedContentType_unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("airport"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()));

        Mockito.verify(topicService, Mockito.never()).createTopic(any());
    }

    @Test
    void unknownEndpoint_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/khong-ton-tai").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ENDPOINT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.path").value("/api/v1/khong-ton-tai"));
    }

    // ===== 500 vẫn giữ cho lỗi không lường trước =====

    @Test
    void unexpectedException_internalServerError() throws Exception {
        Mockito.when(topicService.getTopics()).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/v1/topics").with(user(customUserDetail)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()));
    }
}
