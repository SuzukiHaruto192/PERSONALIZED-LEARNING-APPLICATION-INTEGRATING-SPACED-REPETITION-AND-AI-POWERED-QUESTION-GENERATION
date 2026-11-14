package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.enums.CefrLevel;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.SkillRepository;
import backend.dev.PLA.repositories.quiz.TopicGroupRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import backend.dev.PLA.services.quiz.TopicService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class TopicServiceTest {
    @Mock
    TopicRepository topicRepository;

    @Mock
    TopicGroupRepository topicGroupRepository;

    @Mock
    SkillRepository skillRepository;

    @InjectMocks
    TopicService topicService;

    private TopicGroup group;
    private TopicGroup otherGroup;
    private Topic topic1;
    private Topic topic2;
    private TopicRequest request;
    private Skill grammar;
    private Skill vocab;

    @BeforeEach
    void initData() {
        group = TopicGroup.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .sortOrder(1)
                .build();
        otherGroup = TopicGroup.builder()
                .id(6L)
                .type(TopicGroupType.SITUATION)
                .name("Ẩm thực")
                .sortOrder(2)
                .build();
        topic1 = Topic.builder()
                .id(1L)
                .group(group)
                .slug("airport")
                .nameVi("Sân bay")
                .sortOrder(0)
                .build();
        topic2 = Topic.builder()
                .id(2L)
                .group(group)
                .slug("hotel")
                .nameVi("Khách sạn")
                .sortOrder(1)
                .build();

        grammar = Skill.builder().id((short) 1).code("GRAMMAR").name("Ngữ pháp").build();
        vocab = Skill.builder().id((short) 2).code("VOCAB").name("Từ vựng").build();

        request = TopicRequest.builder()
                .idTopicGroup(5L)
                .slug("  train-station  ")
                .nameVi("  Nhà ga  ")
                .nameEn("  Train station ")
                .keyword("train, ticket")
                .icon("   ")
                .level(CefrLevel.A2)
                .description("Hội thoại ở nhà ga")
                .sortBy(3)
                .skillIds(Set.of((short) 1, (short) 2))
                .build();
    }

    private void stubSkillsFound() {
        when(skillRepository.findAllById(Set.of((short) 1, (short) 2))).thenReturn(List.of(grammar, vocab));
    }

    // ===== getTopics =====

    @Test
    void getTopics_returnsAll() {
        when(topicRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(topic1, topic2));

        List<TopicResponse> response = topicService.getTopics();

        assertEquals(2, response.size());
        assertEquals("airport", response.get(0).getSlug());
        assertEquals("hotel", response.get(1).getSlug());
    }

    @Test
    void getTopics_empty() {
        when(topicRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of());

        assertTrue(topicService.getTopics().isEmpty());
    }

    // ===== getTopicsByGroup =====

    @Test
    void getTopicsByGroup_success() {
        when(topicGroupRepository.existsById(5L)).thenReturn(true);
        when(topicRepository.findByGroupIdOrderBySortOrderAsc(5L)).thenReturn(List.of(topic1, topic2));

        List<TopicResponse> response = topicService.getTopicsByGroup(5L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals(5L, response.get(0).getGroupId());
        assertEquals("Du lịch", response.get(0).getGroupName());
    }

    @Test
    void getTopicsByGroup_groupWithoutTopics_returnsEmpty() {
        when(topicGroupRepository.existsById(5L)).thenReturn(true);
        when(topicRepository.findByGroupIdOrderBySortOrderAsc(5L)).thenReturn(List.of());

        assertTrue(topicService.getTopicsByGroup(5L).isEmpty());
    }

    @Test
    void getTopicsByGroup_groupNotFound() {
        when(topicGroupRepository.existsById(99L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> topicService.getTopicsByGroup(99L));

        assertEquals(ErrorCode.TOPIC_GROUP_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).findByGroupIdOrderBySortOrderAsc(anyLong());
    }

    // ===== getTopicById =====

    @Test
    void getTopicById_success() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));

        TopicResponse response = topicService.getTopicById(1L);

        assertEquals(1L, response.getId());
        assertEquals("airport", response.getSlug());
        assertEquals(5L, response.getGroupId());
        assertTrue(response.getIsActive());
    }

    @Test
    void getTopicById_notFound() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.getTopicById(99L));

        assertEquals(ErrorCode.TOPIC_NOT_FOUND, ex.getErrorCode());
    }

    // ===== createTopic =====

    @Test
    void createTopic_success_trimsAndMapsFields() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlug("train-station")).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TopicResponse response = topicService.createTopic(request);

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).saveAndFlush(captor.capture());
        Topic saved = captor.getValue();
        assertSame(group, saved.getGroup());
        assertEquals("train-station", saved.getSlug());
        assertEquals("Nhà ga", saved.getNameVi());
        assertEquals("Train station", saved.getNameEn());
        assertNull(saved.getIcon());
        assertEquals(CefrLevel.A2, saved.getCefrLevel());
        assertEquals(3, saved.getSortOrder());
        assertTrue(saved.isActive());
        assertEquals(Set.of(grammar, vocab), saved.getSkills());

        assertEquals(10L, response.getId());
        assertEquals(5L, response.getGroupId());
        assertEquals("Du lịch", response.getGroupName());
        assertEquals("train-station", response.getSlug());
        assertEquals("train, ticket", response.getKeyword());
        assertEquals(3, response.getSortBy());
        assertEquals(
                List.of((short) 1, (short) 2),
                response.getSkills().stream().map(sk -> sk.getId()).toList());
    }

    @Test
    void createTopic_nullSortBy_defaultsToZero() {
        request.setSortBy(null);
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlug("train-station")).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        TopicResponse response = topicService.createTopic(request);

        assertEquals(0, response.getSortBy());
    }

    @Test
    void createTopic_groupNotFound() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.createTopic(request));

        assertEquals(ErrorCode.TOPIC_GROUP_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).existsBySlug(anyString());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTopic_slugExisted() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlug("train-station")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> topicService.createTopic(request));

        assertEquals(ErrorCode.TOPIC_SLUG_EXISTED, ex.getErrorCode());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTopic_uniqueConstraintRace_mapsToSlugExisted() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlug("train-station")).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(any(Topic.class))).thenThrow(new DataIntegrityViolationException("uk"));

        AppException ex = assertThrows(AppException.class, () -> topicService.createTopic(request));

        assertEquals(ErrorCode.TOPIC_SLUG_EXISTED, ex.getErrorCode());
    }

    @Test
    void createTopic_skillNotFound() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlug("train-station")).thenReturn(false);
        when(skillRepository.findAllById(Set.of((short) 1, (short) 2))).thenReturn(List.of(grammar));

        AppException ex = assertThrows(AppException.class, () -> topicService.createTopic(request));

        assertEquals(ErrorCode.SKILL_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    // ===== updateTopic =====

    @Test
    void updateTopic_success_changesGroupAndFields() {
        request.setIdTopicGroup(6L);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(6L)).thenReturn(Optional.of(otherGroup));
        when(topicRepository.existsBySlugAndIdNot("train-station", 1L)).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(topic1)).thenReturn(topic1);

        TopicResponse response = topicService.updateTopic(1L, request);

        assertEquals(1L, response.getId());
        assertEquals(6L, response.getGroupId());
        assertEquals("Ẩm thực", response.getGroupName());
        assertEquals("train-station", response.getSlug());
        assertEquals("Nhà ga", response.getNameVi());
        assertEquals(CefrLevel.A2, response.getLevel());
        assertEquals(3, response.getSortBy());
    }

    @Test
    void updateTopic_replacesSkills() {
        topic1.getSkills().add(grammar);
        request.setSkillIds(Set.of((short) 2));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlugAndIdNot("train-station", 1L)).thenReturn(false);
        when(skillRepository.findAllById(Set.of((short) 2))).thenReturn(List.of(vocab));
        when(topicRepository.saveAndFlush(topic1)).thenReturn(topic1);

        TopicResponse response = topicService.updateTopic(1L, request);

        assertEquals(Set.of(vocab), topic1.getSkills());
        assertEquals(1, response.getSkills().size());
        assertEquals("VOCAB", response.getSkills().get(0).getCode());
    }

    @Test
    void updateTopic_skillNotFound() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlugAndIdNot("train-station", 1L)).thenReturn(false);
        when(skillRepository.findAllById(Set.of((short) 1, (short) 2))).thenReturn(List.of());

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopic(1L, request));

        assertEquals(ErrorCode.SKILL_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopic_keepSameSlug_success() {
        request.setSlug("airport");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlugAndIdNot("airport", 1L)).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(topic1)).thenReturn(topic1);

        TopicResponse response = topicService.updateTopic(1L, request);

        assertEquals("airport", response.getSlug());
    }

    @Test
    void updateTopic_topicNotFound() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopic(99L, request));

        assertEquals(ErrorCode.TOPIC_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopic_groupNotFound() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopic(1L, request));

        assertEquals(ErrorCode.TOPIC_GROUP_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopic_slugUsedByOtherTopic() {
        request.setSlug("hotel");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlugAndIdNot("hotel", 1L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopic(1L, request));

        assertEquals(ErrorCode.TOPIC_SLUG_EXISTED, ex.getErrorCode());
        assertEquals("airport", topic1.getSlug());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopic_uniqueConstraintRace_mapsToSlugExisted() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicRepository.existsBySlugAndIdNot("train-station", 1L)).thenReturn(false);
        stubSkillsFound();
        when(topicRepository.saveAndFlush(topic1)).thenThrow(new DataIntegrityViolationException("uk"));

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopic(1L, request));

        assertEquals(ErrorCode.TOPIC_SLUG_EXISTED, ex.getErrorCode());
    }

    // ===== updateTopicActive =====

    @Test
    void updateTopicActive_deactivate() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicRepository.save(topic1)).thenReturn(topic1);

        TopicResponse response = topicService.updateTopicActive(1L, false);

        assertFalse(topic1.isActive());
        assertFalse(response.getIsActive());
    }

    @Test
    void updateTopicActive_activate() {
        topic1.setActive(false);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        when(topicRepository.save(topic1)).thenReturn(topic1);

        TopicResponse response = topicService.updateTopicActive(1L, true);

        assertTrue(response.getIsActive());
    }

    @Test
    void updateTopicActive_notFound() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.updateTopicActive(99L, true));

        assertEquals(ErrorCode.TOPIC_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).save(any());
    }

    // ===== deleteTopic =====

    @Test
    void deleteTopic_success() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));

        assertNull(topicService.deleteTopic(1L));

        verify(topicRepository).delete(topic1);
        verify(topicRepository).flush();
    }

    @Test
    void deleteTopic_notFound() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicService.deleteTopic(99L));

        assertEquals(ErrorCode.TOPIC_NOT_FOUND, ex.getErrorCode());
        verify(topicRepository, never()).delete(any());
    }

    @Test
    void deleteTopic_usedByQuiz_mapsToInUse() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic1));
        doThrow(new DataIntegrityViolationException("fk")).when(topicRepository).flush();

        AppException ex = assertThrows(AppException.class, () -> topicService.deleteTopic(1L));

        assertEquals(ErrorCode.TOPIC_IN_USE, ex.getErrorCode());
    }
}
