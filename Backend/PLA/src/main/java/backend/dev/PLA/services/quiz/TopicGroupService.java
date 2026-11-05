package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.enums.TopicGroupType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.TopicGroupMapper;
import backend.dev.PLA.repositories.quiz.TopicGroupRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicGroupService {

    TopicGroupRepository topicGroupRepository;

    @Transactional(rollbackFor = Exception.class)
    public TopicGroupResponse createTopicGroup(TopicGroupRequest topicGroupRequest) {
        checkExistsTopicGroup(topicGroupRequest);
        TopicGroup topicGroup = TopicGroupMapper.requestToTopicGroup(topicGroupRequest);

        try {
            TopicGroup saved = topicGroupRepository.saveAndFlush(topicGroup);
            return TopicGroupMapper.topicGroupToTopicGroupResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_GROUP_EXISTED);
        }
    }

    @Transactional(readOnly = true)
    public List<TopicGroupResponse> getTopicGroup() {
        List<TopicGroup> topicGroups = topicGroupRepository.findAll();
        List<TopicGroupResponse> topicGroupResponses = new ArrayList<>();
        for (TopicGroup topicGroup : topicGroups) {
            topicGroupResponses.add(TopicGroupMapper.topicGroupToTopicGroupResponse(topicGroup));
        }
        return topicGroupResponses;
    }

    @Transactional(rollbackFor = Exception.class)
    public TopicGroupResponse updateTopicGroup(Long id, UpdateTopicGroupRequest request) {
        TopicGroup topicGroup = getTopicGroup(id);

        String newName = request.getName() != null ? request.getName().trim() : topicGroup.getName();
        TopicGroupType newType = request.getType() != null ? request.getType() : topicGroup.getType();

        if (topicGroupRepository.existsByNameAndTypeAndIdNot(newName, newType, id)) {
            throw new AppException(ErrorCode.TOPIC_GROUP_EXISTED);
        }

        TopicGroupMapper.updateTopicGroup(topicGroup, request);
        try {
            TopicGroup saved = topicGroupRepository.saveAndFlush(topicGroup);
            return TopicGroupMapper.topicGroupToTopicGroupResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_GROUP_EXISTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Void DeleteTopicGroup(Long id) {
        TopicGroup topicGroup = getTopicGroup(id);
        try {
            topicGroupRepository.delete(topicGroup);
            topicGroupRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_GROUP_IN_USE);
        }
        return null;
    }


    private void checkExistsTopicGroup(TopicGroupRequest topicGroupRequest) {
        if (topicGroupRepository.existsByNameAndType(topicGroupRequest.getName().trim(), topicGroupRequest.getType())) {
            throw new AppException(ErrorCode.TOPIC_GROUP_EXISTED);
        }
    }

    private TopicGroup getTopicGroup(Long id) {
        return topicGroupRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND));
    }
}
