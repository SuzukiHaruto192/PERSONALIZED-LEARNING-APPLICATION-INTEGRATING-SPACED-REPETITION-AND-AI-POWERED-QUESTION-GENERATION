package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.TopicGroup;
import java.util.Comparator;

public class TopicMapper {

    public static Topic requestToTopic(TopicRequest request, TopicGroup group) {
        Topic topic = Topic.builder().build();
        updateTopic(topic, request, group);
        return topic;
    }

    public static void updateTopic(Topic topic, TopicRequest request, TopicGroup group) {
        topic.setGroup(group);
        topic.setSlug(request.getSlug().trim());
        topic.setNameVi(request.getNameVi().trim());
        topic.setNameEn(trimToNull(request.getNameEn()));
        topic.setKeywords(trimToNull(request.getKeyword()));
        topic.setIcon(trimToNull(request.getIcon()));
        topic.setCefrLevel(request.getLevel());
        topic.setDescription(trimToNull(request.getDescription()));
        topic.setSortOrder(request.getSortBy() != null ? request.getSortBy() : 0);
    }

    public static TopicResponse topicToTopicResponse(Topic topic) {
        TopicGroup group = topic.getGroup();
        return TopicResponse.builder()
                .id(topic.getId())
                .groupId(group != null ? group.getId() : null)
                .groupName(group != null ? group.getName() : null)
                .slug(topic.getSlug())
                .nameVi(topic.getNameVi())
                .nameEn(topic.getNameEn())
                .keyword(topic.getKeywords())
                .icon(topic.getIcon())
                .level(topic.getCefrLevel())
                .description(topic.getDescription())
                .sortBy(topic.getSortOrder())
                .isActive(topic.isActive())
                .skills(topic.getSkills().stream()
                        .map(SkillMapper::toSkillResponse)
                        .sorted(Comparator.comparing(SkillResponse::getId))
                        .toList())
                .build();
    }

    // Dùng khi topic nằm lồng trong skill/group: bỏ group và skills để tránh lặp dữ liệu
    public static TopicResponse topicToNestedTopicResponse(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .slug(topic.getSlug())
                .nameVi(topic.getNameVi())
                .nameEn(topic.getNameEn())
                .keyword(topic.getKeywords())
                .icon(topic.getIcon())
                .level(topic.getCefrLevel())
                .description(topic.getDescription())
                .sortBy(topic.getSortOrder())
                .isActive(topic.isActive())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
