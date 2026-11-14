package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.SkillRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.services.quiz.SkillService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class SkillServiceTest {
    @Mock
    SkillRepository skillRepository;

    @Mock
    TopicRepository topicRepository;

    @InjectMocks
    SkillService skillService;

    private Skill grammar;
    private Skill vocab;
    private SkillRequest request;

    @BeforeEach
    void initData() {
        grammar = Skill.builder()
                .id(1L)
                .code("GRAMMAR")
                .name("Ngữ pháp")
                .icon("book")
                .build();
        vocab = Skill.builder().id(2L).code("VOCAB").name("Từ vựng").build();

        request = SkillRequest.builder()
                .name("  Nghe  ")
                .code("  LISTENING ")
                .icon("  headphone  ")
                .build();
    }

    // ===== getSkills =====

    @Test
    void getSkills_returnsAll() {
        when(skillRepository.findAll()).thenReturn(List.of(grammar, vocab));

        List<SkillResponse> response = skillService.getSkills();

        assertEquals(2, response.size());
        assertEquals("GRAMMAR", response.get(0).getCode());
        assertEquals("Ngữ pháp", response.get(0).getName());
        assertEquals("book", response.get(0).getIcon());
        assertEquals("VOCAB", response.get(1).getCode());
        assertNull(response.get(0).getTopicGroups());
    }

    @Test
    void getSkills_empty() {
        when(skillRepository.findAll()).thenReturn(List.of());

        assertTrue(skillService.getSkills().isEmpty());
    }

    // ===== getSkillById =====

    @Test
    void getSkillById_groupsTopicsByGroupKeepingQueryOrder() {
        TopicGroup travel = TopicGroup.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .sortOrder(1)
                .build();
        TopicGroup tense = TopicGroup.builder()
                .id(3L)
                .type(TopicGroupType.GRAMMAR)
                .name("Thì hiện tại")
                .sortOrder(2)
                .build();
        Topic airport = Topic.builder()
                .id(10L)
                .group(travel)
                .slug("airport")
                .nameVi("Sân bay")
                .build();
        Topic hotel = Topic.builder()
                .id(11L)
                .group(travel)
                .slug("hotel")
                .nameVi("Khách sạn")
                .build();
        Topic simple = Topic.builder()
                .id(12L)
                .group(tense)
                .slug("present-simple")
                .nameVi("Hiện tại đơn")
                .build();
        airport.getSkills().add(grammar);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        // repository đã sắp xếp theo group.sortOrder, group.id, topic.sortOrder
        when(topicRepository.findBySkillIdWithGroup(1L)).thenReturn(List.of(airport, hotel, simple));

        SkillResponse response = skillService.getSkillById(1L);

        assertEquals(1L, response.getId());
        assertEquals("GRAMMAR", response.getCode());

        List<TopicGroupResponse> groups = response.getTopicGroups();
        assertEquals(2, groups.size());

        assertEquals(5L, groups.get(0).getId());
        assertEquals("Du lịch", groups.get(0).getName());
        assertEquals(TopicGroupType.SITUATION, groups.get(0).getType());
        assertEquals(
                List.of("airport", "hotel"),
                groups.get(0).getTopics().stream().map(t -> t.getSlug()).toList());

        assertEquals(3L, groups.get(1).getId());
        assertEquals(
                List.of("present-simple"),
                groups.get(1).getTopics().stream().map(t -> t.getSlug()).toList());

        // topic lồng trong group không lặp lại thông tin group / skills
        assertNull(groups.get(0).getTopics().get(0).getGroupId());
        assertNull(groups.get(0).getTopics().get(0).getSkills());
    }

    @Test
    void getSkillById_withoutTopics_returnsEmptyGroups() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(topicRepository.findBySkillIdWithGroup(1L)).thenReturn(List.of());

        SkillResponse response = skillService.getSkillById(1L);

        assertEquals("GRAMMAR", response.getCode());
        assertTrue(response.getTopicGroups().isEmpty());
    }

    @Test
    void getSkillById_notFound() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> skillService.getSkillById(99L));

        assertEquals(ErrorCode.SKILL_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).findBySkillIdWithGroup(anyLong());
    }

    // ===== saveSkill =====

    @Test
    void saveSkill_success_trimsFields() {
        when(skillRepository.existsByCode("LISTENING")).thenReturn(false);
        when(skillRepository.saveAndFlush(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(3L);
            return s;
        });

        SkillResponse response = skillService.saveSkill(request);

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).saveAndFlush(captor.capture());
        Skill saved = captor.getValue();
        assertEquals("Nghe", saved.getName());
        assertEquals("LISTENING", saved.getCode());
        assertEquals("headphone", saved.getIcon());

        assertEquals(3L, response.getId());
        assertEquals("LISTENING", response.getCode());
        assertEquals("Nghe", response.getName());
    }

    @Test
    void saveSkill_nullIcon_isAllowed() {
        request.setIcon(null);
        when(skillRepository.existsByCode("LISTENING")).thenReturn(false);
        when(skillRepository.saveAndFlush(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillResponse response = skillService.saveSkill(request);

        assertNull(response.getIcon());
    }

    @Test
    void saveSkill_codeExisted() {
        when(skillRepository.existsByCode("LISTENING")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> skillService.saveSkill(request));

        assertEquals(ErrorCode.SKILL_CODE_EXISTED, ex.getErrorCode());
        verify(skillRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveSkill_uniqueConstraintRace_mapsToCodeExisted() {
        when(skillRepository.existsByCode("LISTENING")).thenReturn(false);
        when(skillRepository.saveAndFlush(any(Skill.class))).thenThrow(new DataIntegrityViolationException("uk"));

        AppException ex = assertThrows(AppException.class, () -> skillService.saveSkill(request));

        assertEquals(ErrorCode.SKILL_CODE_EXISTED, ex.getErrorCode());
    }

    // ===== updateSkill =====

    @Test
    void updateSkill_success() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(skillRepository.existsByCodeAndIdNot("LISTENING", 1L)).thenReturn(false);
        when(skillRepository.saveAndFlush(grammar)).thenReturn(grammar);

        SkillResponse response = skillService.updateSkill(1L, request);

        assertEquals("Nghe", grammar.getName());
        assertEquals("LISTENING", grammar.getCode());
        assertEquals("headphone", grammar.getIcon());
        assertEquals(1L, response.getId());
        assertEquals("LISTENING", response.getCode());
    }

    @Test
    void updateSkill_keepSameCode_success() {
        request.setCode("GRAMMAR");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(skillRepository.existsByCodeAndIdNot("GRAMMAR", 1L)).thenReturn(false);
        when(skillRepository.saveAndFlush(grammar)).thenReturn(grammar);

        SkillResponse response = skillService.updateSkill(1L, request);

        assertEquals("GRAMMAR", response.getCode());
        assertEquals("Nghe", response.getName());
    }

    @Test
    void updateSkill_nullIcon_clearsIcon() {
        request.setIcon(null);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(skillRepository.existsByCodeAndIdNot("LISTENING", 1L)).thenReturn(false);
        when(skillRepository.saveAndFlush(grammar)).thenReturn(grammar);

        skillService.updateSkill(1L, request);

        assertNull(grammar.getIcon());
    }

    @Test
    void updateSkill_notFound() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> skillService.updateSkill(99L, request));

        assertEquals(ErrorCode.SKILL_NOT_FOUND, ex.getErrorCode());
        verify(skillRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSkill_codeUsedByOtherSkill() {
        request.setCode("VOCAB");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(skillRepository.existsByCodeAndIdNot("VOCAB", 1L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> skillService.updateSkill(1L, request));

        assertEquals(ErrorCode.SKILL_CODE_EXISTED, ex.getErrorCode());
        assertEquals("GRAMMAR", grammar.getCode());
        verify(skillRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSkill_uniqueConstraintRace_mapsToCodeExisted() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        when(skillRepository.existsByCodeAndIdNot("LISTENING", 1L)).thenReturn(false);
        when(skillRepository.saveAndFlush(grammar)).thenThrow(new DataIntegrityViolationException("uk"));

        AppException ex = assertThrows(AppException.class, () -> skillService.updateSkill(1L, request));

        assertEquals(ErrorCode.SKILL_CODE_EXISTED, ex.getErrorCode());
    }

    // ===== deleteSkill =====

    @Test
    void deleteSkill_success() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));

        assertNull(skillService.deleteSkill(1L));

        verify(skillRepository).delete(grammar);
        verify(skillRepository).flush();
    }

    @Test
    void deleteSkill_notFound() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> skillService.deleteSkill(99L));

        assertEquals(ErrorCode.SKILL_NOT_FOUND, ex.getErrorCode());
        verify(skillRepository, never()).delete(any());
    }

    @Test
    void deleteSkill_referenced_mapsToInUse() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(grammar));
        doThrow(new DataIntegrityViolationException("fk")).when(skillRepository).flush();

        AppException ex = assertThrows(AppException.class, () -> skillService.deleteSkill(1L));

        assertEquals(ErrorCode.SKILL_IN_USE, ex.getErrorCode());
    }
}
