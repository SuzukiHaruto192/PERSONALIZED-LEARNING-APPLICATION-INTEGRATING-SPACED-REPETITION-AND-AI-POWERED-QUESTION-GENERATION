package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.TopicGroupService;
import backend.dev.PLA.services.quiz.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class TopicGroupControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicGroupService topicGroupService;

    @MockitoBean
    private TopicService topicService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetail customUserDetail;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(
                User.builder().id(1L).username("test").password("123456").build());
    }

    // ===== GET /api/v1/topic-groups/{id}/topics =====

    @Test
    void getTopicsByGroup_success() throws Exception {
        TopicResponse topic = TopicResponse.builder()
                .id(1L)
                .groupId(5L)
                .groupName("Du lịch")
                .slug("airport")
                .nameVi("Sân bay")
                .build();
        Mockito.when(topicService.getTopicsByGroup(5L)).thenReturn(List.of(topic));

        mockMvc.perform(get("/api/v1/topic-groups/5/topics").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].groupId").value(5))
                .andExpect(jsonPath("$.data[0].slug").value("airport"));
    }

    @Test
    void getTopicsByGroup_groupNotFound() throws Exception {
        Mockito.when(topicService.getTopicsByGroup(99L)).thenThrow(new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND));

        mockMvc.perform(get("/api/v1/topic-groups/99/topics").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_NOT_FOUND.getCode()));
    }

    @Test
    void getTopicsByGroup_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/topic-groups/5/topics")).andExpect(status().isUnauthorized());

        Mockito.verify(topicService, Mockito.never()).getTopicsByGroup(anyLong());
    }

    // ===== PATCH /api/v1/topic-groups/{id} =====

    @Test
    void updateTopicGroup_success() throws Exception {
        UpdateTopicGroupRequest request =
                UpdateTopicGroupRequest.builder().icon("bus").build();
        TopicGroupResponse response = TopicGroupResponse.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .icon("bus")
                .orderBy(1)
                .build();
        Mockito.when(topicGroupService.updateTopicGroup(eq(5L), any(UpdateTopicGroupRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/topic-groups/5")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.icon").value("bus"))
                .andExpect(jsonPath("$.data.name").value("Du lịch"));
    }

    @Test
    void updateTopicGroup_blankName_badRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/topic-groups/5")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        Mockito.verify(topicGroupService, Mockito.never()).updateTopicGroup(anyLong(), any());
    }

    @Test
    void updateTopicGroup_notFound() throws Exception {
        Mockito.when(topicGroupService.updateTopicGroup(eq(99L), any(UpdateTopicGroupRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/topic-groups/99")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ẩm thực\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_NOT_FOUND.getCode()));
    }

    @Test
    void updateTopicGroup_duplicate() throws Exception {
        Mockito.when(topicGroupService.updateTopicGroup(eq(5L), any(UpdateTopicGroupRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_GROUP_EXISTED));

        mockMvc.perform(patch("/api/v1/topic-groups/5")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ẩm thực\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_EXISTED.getCode()));
    }

    // ===== POST /api/v1/topic-groups =====

    @Test
    void createTopicGroup_success_readsJsonBody() throws Exception {
        TopicGroupRequest request = TopicGroupRequest.builder()
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .icon("plane")
                .sortOrder(1)
                .build();
        TopicGroupResponse response = TopicGroupResponse.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .icon("plane")
                .orderBy(1)
                .build();
        Mockito.when(topicGroupService.createTopicGroup(any(TopicGroupRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/topic-groups")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Du lịch"));

        ArgumentCaptor<TopicGroupRequest> captor = ArgumentCaptor.forClass(TopicGroupRequest.class);
        Mockito.verify(topicGroupService).createTopicGroup(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Du lịch", captor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals(
                TopicGroupType.SITUATION, captor.getValue().getType());
    }

    @Test
    void createTopicGroup_missingFields_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/topic-groups")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.sortOrder").exists());

        Mockito.verify(topicGroupService, Mockito.never()).createTopicGroup(any());
    }

    @Test
    void createTopicGroup_duplicate() throws Exception {
        Mockito.when(topicGroupService.createTopicGroup(any(TopicGroupRequest.class)))
                .thenThrow(new AppException(ErrorCode.TOPIC_GROUP_EXISTED));

        mockMvc.perform(post("/api/v1/topic-groups")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SITUATION\",\"name\":\"Du lịch\",\"sortOrder\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_EXISTED.getCode()));
    }

    // ===== DELETE /api/v1/topic-groups/{id} =====

    @Test
    void deleteTopicGroup_success_usesPathVariable() throws Exception {
        mockMvc.perform(delete("/api/v1/topic-groups/5").with(user(customUserDetail)))
                .andExpect(status().isOk());

        Mockito.verify(topicGroupService).DeleteTopicGroup(5L);
    }

    @Test
    void deleteTopicGroup_stillHasTopics() throws Exception {
        Mockito.when(topicGroupService.DeleteTopicGroup(5L)).thenThrow(new AppException(ErrorCode.TOPIC_GROUP_IN_USE));

        mockMvc.perform(delete("/api/v1/topic-groups/5").with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOPIC_GROUP_IN_USE.getCode()));
    }
}
