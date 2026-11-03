package backend.dev.PLA.service;

import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.TopicGroup;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.TopicGroupRepository;
import backend.dev.PLA.services.quiz.TopicGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TopicGroupServiceTest {
    @Mock
    TopicGroupRepository topicGroupRepository;

    @InjectMocks
    TopicGroupService topicGroupService;

    private TopicGroup group;

    @BeforeEach
    void initData() {
        group = TopicGroup.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .icon("plane")
                .sortOrder(1)
                .build();
    }

    // ===== updateTopicGroup =====

    @Test
    void updateTopicGroup_allFields_success() {
        UpdateTopicGroupRequest request = UpdateTopicGroupRequest.builder()
                .type(TopicGroupType.GRAMMAR)
                .name("  Thì hiện tại  ")
                .icon("clock")
                .sortOrder(3)
                .build();
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicGroupRepository.existsByNameAndTypeAndIdNot("Thì hiện tại", TopicGroupType.GRAMMAR, 5L))
                .thenReturn(false);
        when(topicGroupRepository.saveAndFlush(group)).thenReturn(group);

        TopicGroupResponse response = topicGroupService.updateTopicGroup(5L, request);

        assertEquals(5L, response.getId());
        assertEquals(TopicGroupType.GRAMMAR, response.getType());
        assertEquals("Thì hiện tại", response.getName());
        assertEquals("clock", response.getIcon());
        assertEquals(3, response.getOrderBy());
    }

    @Test
    void updateTopicGroup_partial_keepsNullFieldsUnchanged() {
        UpdateTopicGroupRequest request = UpdateTopicGroupRequest.builder().icon("bus").build();
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicGroupRepository.existsByNameAndTypeAndIdNot("Du lịch", TopicGroupType.SITUATION, 5L))
                .thenReturn(false);
        when(topicGroupRepository.saveAndFlush(group)).thenReturn(group);

        TopicGroupResponse response = topicGroupService.updateTopicGroup(5L, request);

        assertEquals("bus", response.getIcon());
        assertEquals("Du lịch", response.getName());
        assertEquals(TopicGroupType.SITUATION, response.getType());
        assertEquals(1, response.getOrderBy());
    }

    @Test
    void updateTopicGroup_notFound() {
        when(topicGroupRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> topicGroupService.updateTopicGroup(99L, UpdateTopicGroupRequest.builder().name("x").build()));

        assertEquals(ErrorCode.TOPIC_GROUP_NOT_FOUND, ex.getErrorCode());
        verify(topicGroupRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopicGroup_duplicateNameAndType() {
        UpdateTopicGroupRequest request = UpdateTopicGroupRequest.builder().name("Ẩm thực").build();
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicGroupRepository.existsByNameAndTypeAndIdNot("Ẩm thực", TopicGroupType.SITUATION, 5L))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> topicGroupService.updateTopicGroup(5L, request));

        assertEquals(ErrorCode.TOPIC_GROUP_EXISTED, ex.getErrorCode());
        verify(topicGroupRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTopicGroup_onlyTypeChanged_checksDuplicateWithOldName() {
        UpdateTopicGroupRequest request = UpdateTopicGroupRequest.builder().type(TopicGroupType.GRAMMAR).build();
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicGroupRepository.existsByNameAndTypeAndIdNot("Du lịch", TopicGroupType.GRAMMAR, 5L))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> topicGroupService.updateTopicGroup(5L, request));

        assertEquals(ErrorCode.TOPIC_GROUP_EXISTED, ex.getErrorCode());
    }

    @Test
    void updateTopicGroup_uniqueConstraintRace_mapsToExisted() {
        UpdateTopicGroupRequest request = UpdateTopicGroupRequest.builder().name("Ẩm thực").build();
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(topicGroupRepository.existsByNameAndTypeAndIdNot("Ẩm thực", TopicGroupType.SITUATION, 5L))
                .thenReturn(false);
        when(topicGroupRepository.saveAndFlush(group)).thenThrow(new DataIntegrityViolationException("uk"));

        AppException ex = assertThrows(AppException.class,
                () -> topicGroupService.updateTopicGroup(5L, request));

        assertEquals(ErrorCode.TOPIC_GROUP_EXISTED, ex.getErrorCode());
    }

    // ===== DeleteTopicGroup =====

    @Test
    void deleteTopicGroup_success() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));

        topicGroupService.DeleteTopicGroup(5L);

        verify(topicGroupRepository).delete(group);
        verify(topicGroupRepository).flush();
    }

    @Test
    void deleteTopicGroup_notFound() {
        when(topicGroupRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> topicGroupService.DeleteTopicGroup(99L));

        assertEquals(ErrorCode.TOPIC_GROUP_NOT_FOUND, ex.getErrorCode());
        verify(topicGroupRepository, never()).delete(any());
    }

    @Test
    void deleteTopicGroup_stillHasTopics_mapsToInUse() {
        when(topicGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        doThrow(new DataIntegrityViolationException("fk")).when(topicGroupRepository).flush();

        AppException ex = assertThrows(AppException.class, () -> topicGroupService.DeleteTopicGroup(5L));

        assertEquals(ErrorCode.TOPIC_GROUP_IN_USE, ex.getErrorCode());
    }
}
