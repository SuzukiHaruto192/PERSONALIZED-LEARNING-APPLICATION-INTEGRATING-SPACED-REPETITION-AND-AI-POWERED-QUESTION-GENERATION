package backend.dev.PLA.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SkillControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillService skillService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetail customUserDetail;
    private SkillRequest request;
    private SkillResponse response;

    @BeforeEach
    void initData() {
        customUserDetail = new CustomUserDetail(
                User.builder().id(1L).username("test").password("123456").build());

        request = SkillRequest.builder()
                .name("Ngữ pháp")
                .code("GRAMMAR")
                .icon("book")
                .build();

        response = SkillResponse.builder()
                .id(1L)
                .name("Ngữ pháp")
                .code("GRAMMAR")
                .icon("book")
                .build();
    }

    // ===== GET /api/v1/skills =====

    @Test
    void getSkills_success() throws Exception {
        Mockito.when(skillService.getSkills()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/skills").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("GRAMMAR"))
                .andExpect(jsonPath("$.data[0].name").value("Ngữ pháp"));
    }

    @Test
    void getSkills_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/skills")).andExpect(status().isUnauthorized());

        Mockito.verify(skillService, Mockito.never()).getSkills();
    }

    // ===== GET /api/v1/skills/{id} =====

    @Test
    void getSkill_success_returnsNestedGroupsAndTopics() throws Exception {
        TopicResponse topic = TopicResponse.builder()
                .id(10L)
                .slug("present-simple")
                .nameVi("Hiện tại đơn")
                .build();
        TopicGroupResponse group = TopicGroupResponse.builder()
                .id(3L)
                .type(TopicGroupType.GRAMMAR)
                .name("Thì hiện tại")
                .topics(List.of(topic))
                .build();
        response.setTopicGroups(List.of(group));
        Mockito.when(skillService.getSkillById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/skills/1").with(user(customUserDetail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.topicGroups", hasSize(1)))
                .andExpect(jsonPath("$.data.topicGroups[0].name").value("Thì hiện tại"))
                .andExpect(jsonPath("$.data.topicGroups[0].type").value("GRAMMAR"))
                .andExpect(jsonPath("$.data.topicGroups[0].topics[0].slug").value("present-simple"));
    }

    @Test
    void getSkill_notFound() throws Exception {
        Mockito.when(skillService.getSkillById(99L)).thenThrow(new AppException(ErrorCode.SKILL_NOT_FOUND));

        mockMvc.perform(get("/api/v1/skills/99").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_NOT_FOUND.getCode()));
    }

    // ===== POST /api/v1/skills =====

    @Test
    void createSkill_success() throws Exception {
        Mockito.when(skillService.saveSkill(any(SkillRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("GRAMMAR"));
    }

    @Test
    void createSkill_invalidCode_badRequest() throws Exception {
        request.setCode("grammar");

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.code").exists());

        Mockito.verify(skillService, Mockito.never()).saveSkill(any());
    }

    @Test
    void createSkill_missingRequiredFields_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.code").exists());

        Mockito.verify(skillService, Mockito.never()).saveSkill(any());
    }

    @Test
    void createSkill_codeExisted() throws Exception {
        Mockito.when(skillService.saveSkill(any(SkillRequest.class)))
                .thenThrow(new AppException(ErrorCode.SKILL_CODE_EXISTED));

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_CODE_EXISTED.getCode()));
    }

    @Test
    void createSkill_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        Mockito.verify(skillService, Mockito.never()).saveSkill(any());
    }

    // ===== PUT /api/v1/skills/{id} =====

    @Test
    void updateSkill_success() throws Exception {
        Mockito.when(skillService.updateSkill(eq(1L), any(SkillRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/skills/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateSkill_nameTooLong_badRequest() throws Exception {
        request.setName("a".repeat(51));

        mockMvc.perform(put("/api/v1/skills/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        Mockito.verify(skillService, Mockito.never()).updateSkill(anyLong(), any());
    }

    @Test
    void updateSkill_notFound() throws Exception {
        Mockito.when(skillService.updateSkill(eq(99L), any(SkillRequest.class)))
                .thenThrow(new AppException(ErrorCode.SKILL_NOT_FOUND));

        mockMvc.perform(put("/api/v1/skills/99")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_NOT_FOUND.getCode()));
    }

    @Test
    void updateSkill_codeExisted() throws Exception {
        Mockito.when(skillService.updateSkill(eq(1L), any(SkillRequest.class)))
                .thenThrow(new AppException(ErrorCode.SKILL_CODE_EXISTED));

        mockMvc.perform(put("/api/v1/skills/1")
                        .with(user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_CODE_EXISTED.getCode()));
    }

    // ===== DELETE /api/v1/skills/{id} =====

    @Test
    void deleteSkill_success() throws Exception {
        mockMvc.perform(delete("/api/v1/skills/1").with(user(customUserDetail))).andExpect(status().isOk());

        Mockito.verify(skillService).deleteSkill(1L);
    }

    @Test
    void deleteSkill_notFound() throws Exception {
        Mockito.when(skillService.deleteSkill(99L)).thenThrow(new AppException(ErrorCode.SKILL_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/skills/99").with(user(customUserDetail)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_NOT_FOUND.getCode()));
    }

    @Test
    void deleteSkill_inUse() throws Exception {
        Mockito.when(skillService.deleteSkill(1L)).thenThrow(new AppException(ErrorCode.SKILL_IN_USE));

        mockMvc.perform(delete("/api/v1/skills/1").with(user(customUserDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.SKILL_IN_USE.getCode()));
    }
}
