package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.SkillMapper;
import backend.dev.PLA.mapper.TopicGroupMapper;
import backend.dev.PLA.mapper.TopicMapper;
import backend.dev.PLA.repositories.quiz.SkillRepository;
import backend.dev.PLA.repositories.quiz.TopicRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillService {
    SkillRepository skillRepository;
    TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills() {
        return skillRepository.findAll().stream()
                .map(SkillMapper::toSkillResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        Skill skill = getSkill(id);

        Map<TopicGroup, List<Topic>> topicsByGroup = topicRepository.findBySkillIdWithGroup(id).stream()
                .collect(Collectors.groupingBy(Topic::getGroup, LinkedHashMap::new, Collectors.toList()));

        List<TopicGroupResponse> topicGroups = topicsByGroup.entrySet().stream()
                .map(entry -> {
                    TopicGroupResponse groupResponse = TopicGroupMapper.topicGroupToTopicGroupResponse(entry.getKey());
                    groupResponse.setTopics(entry.getValue().stream()
                            .map(TopicMapper::topicToNestedTopicResponse)
                            .toList());
                    return groupResponse;
                })
                .toList();

        SkillResponse response = SkillMapper.toSkillResponse(skill);
        response.setTopicGroups(topicGroups);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public SkillResponse saveSkill(SkillRequest skillRequest) {
        normalize(skillRequest);
        checkSkillExists(skillRequest);

        Skill skill = SkillMapper.toSkill(skillRequest);
        try {
            return SkillMapper.toSkillResponse(skillRepository.saveAndFlush(skill));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.SKILL_CODE_EXISTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SkillResponse updateSkill(Long id, SkillRequest skillRequest) {
        Skill skill = getSkill(id);
        normalize(skillRequest);

        if (skillRepository.existsByCodeAndIdNot(skillRequest.getCode(), id)) {
            throw new AppException(ErrorCode.SKILL_CODE_EXISTED);
        }

        SkillMapper.updateSkill(skill, skillRequest);
        try {
            return SkillMapper.toSkillResponse(skillRepository.saveAndFlush(skill));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.SKILL_CODE_EXISTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Void deleteSkill(Long id) {
        Skill skill = getSkill(id);
        try {
            skillRepository.delete(skill);
            skillRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // skill đang được tham chiếu bởi quiz / quiz_format / skill_topics / user_topic_progress
            throw new AppException(ErrorCode.SKILL_IN_USE);
        }
        return null;
    }

    private void checkSkillExists(SkillRequest skillRequest) {
        if (skillRepository.existsByCode(skillRequest.getCode())) {
            throw new AppException(ErrorCode.SKILL_CODE_EXISTED);
        }
    }

    private Skill getSkill(Long id) {
        return skillRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));
    }

    private void normalize(SkillRequest skillRequest) {
        skillRequest.setName(skillRequest.getName().trim());
        skillRequest.setCode(skillRequest.getCode().trim());
        if (skillRequest.getIcon() != null) {
            skillRequest.setIcon(skillRequest.getIcon().trim());
        }
    }
}
