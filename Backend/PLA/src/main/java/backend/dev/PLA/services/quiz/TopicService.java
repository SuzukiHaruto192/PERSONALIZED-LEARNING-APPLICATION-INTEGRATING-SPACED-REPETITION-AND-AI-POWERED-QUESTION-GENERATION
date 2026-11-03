package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.Topic;
import backend.dev.PLA.entities.TopicGroup;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.TopicMapper;
import backend.dev.PLA.repositories.TopicGroupRepository;
import backend.dev.PLA.repositories.TopicRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor()
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class TopicService {
    TopicRepository topicRepository;
    TopicGroupRepository topicGroupRepository;

    @Transactional(rollbackFor = Exception.class)
    public TopicResponse createTopic(TopicRequest request) {
        TopicGroup group = findTopicGroupById(request.getIdTopicGroup());
        if (topicRepository.existsBySlug(normalizeSlug(request.getSlug()))) {
            throw new AppException(ErrorCode.TOPIC_SLUG_EXISTED);
        }

        Topic topic = TopicMapper.requestToTopic(request, group);
        try {
            return TopicMapper.topicToTopicResponse(topicRepository.saveAndFlush(topic));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_SLUG_EXISTED);
        }
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopics() {
        return topicRepository.findAllByOrderBySortOrderAsc().stream()
                .map(TopicMapper::topicToTopicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByGroup(Long groupId) {
        if (!topicGroupRepository.existsById(groupId)) {
            throw new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND);
        }
        return topicRepository.findByGroupIdOrderBySortOrderAsc(groupId).stream()
                .map(TopicMapper::topicToTopicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Long id) {
        return TopicMapper.topicToTopicResponse(findTopicById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public TopicResponse updateTopic(Long id, TopicRequest request) {
        Topic topic = findTopicById(id);
        TopicGroup group = findTopicGroupById(request.getIdTopicGroup());
        if (topicRepository.existsBySlugAndIdNot(normalizeSlug(request.getSlug()), id)) {
            throw new AppException(ErrorCode.TOPIC_SLUG_EXISTED);
        }

        TopicMapper.updateTopic(topic, request, group);
        try {
            return TopicMapper.topicToTopicResponse(topicRepository.saveAndFlush(topic));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_SLUG_EXISTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public TopicResponse updateTopicActive(Long id, boolean active) {
        Topic topic = findTopicById(id);
        topic.setActive(active);
        return TopicMapper.topicToTopicResponse(topicRepository.save(topic));
    }

    @Transactional(rollbackFor = Exception.class)
    public Void deleteTopic(Long id) {
        Topic topic = findTopicById(id);
        try {
            topicRepository.delete(topic);
            topicRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.TOPIC_IN_USE);
        }
        return null;
    }

    private Topic findTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private TopicGroup findTopicGroupById(Long groupId) {
        return topicGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_GROUP_NOT_FOUND));
    }

    private String normalizeSlug(String slug) {
        return slug.trim();
    }
}
