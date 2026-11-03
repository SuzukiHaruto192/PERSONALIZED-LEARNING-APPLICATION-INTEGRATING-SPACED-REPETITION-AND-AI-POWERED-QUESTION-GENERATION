package backend.dev.PLA.controller;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.enums.CefrLevel;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TopicControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicService topicService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetail customUserDetail;
    private TopicRequest request;
    private TopicResponse response;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(User.builder()
                .id(1L)
                .username("test")
                .password("123456")
                .build());

        request = TopicRequest.builder()
                .idTopicGroup(5L)
                .slug("airport")
                .nameVi("Sân bay")
                .nameEn("Airport")
                .level(CefrLevel.A2)
                .sortBy(0)
                .build();

        response = TopicResponse.builder()
                .id(1L).groupId(5L).groupName("Du lịch").slug("airport").nameVi("Sân bay")
                .nameEn("Airport").level(CefrLevel.A2).sortBy(0).isActive(true).build();
    }

    // ===== GET /api/v1/topics =====

    @Test
    void getTopics_success() throws Exception {
        Mockito.when(topicService.getTopics()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/topics").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].slug").value("airport"))
                .andExpect(jsonPath("$.data[0].level").value("A2"));
    }

    @Test
    void getTopics_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isUnauthorized());

        Mockito.verify(topicService, Mockito.never()).getTopics();
    }

    // ===== GET /api/v1/topics/{id} =====

    @Test
    void getTopic_success() throws Exception {
        Mockito.when(topicService.getTopicById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/topics/1").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.groupName").value("Du lịch"));
    }

    @Test
    void getTopic_notFound() throws Exception {
        Mockito.when(topicService.getTopicById(99L)).thenThrow(new AppException(ErrorCode.TOPIC_NOT_FOUND));

        mockMvc.perform(get("/api/v1/topics/99").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_NOT_FOUND.getCode()));
    }

    // ===== POST /api/v1/topics =====

    @Test
    void createTopic_success() throws Exception {
        Mockito.when(topicService.createTopic(any(TopicRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.slug").value("airport"));
    }

    @Test
    void createTopic_invalidSlug_badRequest() throws Exception {
        request.setSlug("Sân Bay");

        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.slug").exists());

        Mockito.verify(topicService, Mockito.never()).createTopic(any());
    }

    @Test
    void createTopic_missingRequiredFields_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idTopicGroup").exists())
                .andExpect(jsonPath("$.errors.slug").exists())
                .andExpect(jsonPath("$.errors.nameVi").exists())
                .andExpect(jsonPath("$.errors.level").exists());

        Mockito.verify(topicService, Mockito.never()).createTopic(any());
    }

    @Test
    void createTopic_slugExisted() throws Exception {
        Mockito.when(topicService.createTopic(any(TopicRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_SLUG_EXISTED));

        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_SLUG_EXISTED.getCode()));
    }

    @Test
    void createTopic_groupNotFound() throws Exception {
        Mockito.when(topicService.createTopic(any(TopicRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND));

        mockMvc.perform(post("/api/v1/topics")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_NOT_FOUND.getCode()));
    }

    // ===== PUT /api/v1/topics/{id} =====

    @Test
    void updateTopic_success() throws Exception {
        Mockito.when(topicService.updateTopic(eq(1L), any(TopicRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/topics/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateTopic_negativeSortBy_badRequest() throws Exception {
        request.setSortBy(-1);

        mockMvc.perform(put("/api/v1/topics/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sortBy").exists());

        Mockito.verify(topicService, Mockito.never()).updateTopic(anyLong(), any());
    }

    @Test
    void updateTopic_notFound() throws Exception {
        Mockito.when(topicService.updateTopic(eq(99L), any(TopicRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_NOT_FOUND));

        mockMvc.perform(put("/api/v1/topics/99")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_NOT_FOUND.getCode()));
    }

    // ===== PATCH /api/v1/topics/{id}/active =====

    @Test
    void updateTopicActive_success() throws Exception {
        response.setIsActive(false);
        Mockito.when(topicService.updateTopicActive(1L, false)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/topics/1/active")
                        .param("active", "false")
                        .with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        Mockito.verify(topicService).updateTopicActive(1L, false);
    }

    @Test
    void updateTopicActive_missingActiveParam_defaultsToTrue() throws Exception {
        Mockito.when(topicService.updateTopicActive(1L, true)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/topics/1/active").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));

        Mockito.verify(topicService).updateTopicActive(1L, true);
    }

    @Test
    void updateTopicActive_invalidActiveValue_badRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/topics/1/active")
                        .param("active", "abc")
                        .with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.active").exists());

        Mockito.verify(topicService, Mockito.never()).updateTopicActive(anyLong(), Mockito.anyBoolean());
    }

    @Test
    void updateTopicActive_notFound() throws Exception {
        Mockito.when(topicService.updateTopicActive(99L, true))
                .thenThrow(new AppException(ErrorCode.TOPIC_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/topics/99/active")
                        .param("active", "true")
                        .with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_NOT_FOUND.getCode()));
    }

    // ===== DELETE /api/v1/topics/{id} =====

    @Test
    void deleteTopic_success() throws Exception {
        mockMvc.perform(delete("/api/v1/topics/1").with(user(customUserDetail)))
                .andExpect(status().isOk());

        Mockito.verify(topicService).deleteTopic(1L);
    }

    @Test
    void deleteTopic_inUse() throws Exception {
        Mockito.when(topicService.deleteTopic(1L)).thenThrow(new AppException(ErrorCode.TOPIC_IN_USE));

        mockMvc.perform(delete("/api/v1/topics/1").with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_IN_USE.getCode()));
    }
}
