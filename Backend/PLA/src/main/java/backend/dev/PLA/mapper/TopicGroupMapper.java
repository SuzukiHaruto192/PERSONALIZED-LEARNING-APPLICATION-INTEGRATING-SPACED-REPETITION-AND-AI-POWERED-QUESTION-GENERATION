package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.TopicGroup;

public class TopicGroupMapper {
    public static TopicGroup requestToTopicGroup(TopicGroupRequest topicGroupRequest ) {
        return TopicGroup.builder()
                .id(null)
                .topics(null)
                .type(topicGroupRequest.getType())
                .icon(topicGroupRequest.getIcon())
                .name(topicGroupRequest.getName().trim())
                .sortOrder(topicGroupRequest.getSortOrder())
                .build();
    }

    public static TopicGroupResponse topicGroupToTopicGroupResponse(TopicGroup topicGroup) {
        return TopicGroupResponse.builder()
                .id(topicGroup.getId())
                .icon(topicGroup.getIcon())
                .type(topicGroup.getType())
                .name(topicGroup.getName())
                .orderBy(topicGroup.getSortOrder())
                .build();
    }
}
