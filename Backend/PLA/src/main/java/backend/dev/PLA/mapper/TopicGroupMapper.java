package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.TopicGroupRequest;
import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicGroupResponse;
import backend.dev.PLA.entities.quiz.TopicGroup;

public class TopicGroupMapper {
    public static TopicGroup requestToTopicGroup(TopicGroupRequest topicGroupRequest) {
        return TopicGroup.builder()
                .id(null)
                .topics(null)
                .type(topicGroupRequest.getType())
                .icon(topicGroupRequest.getIcon())
                .name(topicGroupRequest.getName().trim())
                .sortOrder(topicGroupRequest.getSortOrder())
                .build();
    }

//    public static TopicGroupResponse toTopicGroupResponse(TopicGroup topicGroup) {
//        return TopicGroupResponse.builder()
//                .id(topicGroup.getId())
//                .
//    }

    public static void updateTopicGroup(TopicGroup topicGroup, UpdateTopicGroupRequest request) {
        if (request.getType() != null) {
            topicGroup.setType(request.getType());
        }
        if (request.getName() != null) {
            topicGroup.setName(request.getName().trim());
        }
        if (request.getIcon() != null) {
            topicGroup.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            topicGroup.setSortOrder(request.getSortOrder());
        }
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
