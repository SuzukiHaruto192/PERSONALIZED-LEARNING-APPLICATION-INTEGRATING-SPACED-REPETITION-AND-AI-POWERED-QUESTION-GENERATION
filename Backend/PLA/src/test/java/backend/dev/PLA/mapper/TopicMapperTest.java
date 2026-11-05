package backend.dev.PLA.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.dev.PLA.dto.request.Quiz.TopicRequest;
import backend.dev.PLA.dto.request.Quiz.UpdateTopicGroupRequest;
import backend.dev.PLA.dto.response.quiz.TopicResponse;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.TopicGroup;
import backend.dev.PLA.enums.CefrLevel;
import backend.dev.PLA.enums.TopicGroupType;
import org.junit.jupiter.api.Test;

class TopicMapperTest {

    private final TopicGroup group = TopicGroup.builder()
            .id(5L)
            .type(TopicGroupType.SITUATION)
            .name("Du lịch")
            .icon("plane")
            .sortOrder(1)
            .build();

    @Test
    void requestToTopic_trimsAndBlankOptionalBecomesNull() {
        TopicRequest request = TopicRequest.builder()
                .idTopicGroup(5L)
                .slug(" airport ")
                .nameVi(" Sân bay ")
                .nameEn("   ")
                .keyword(null)
                .icon(" plane ")
                .level(CefrLevel.B1)
                .description("")
                .sortBy(null)
                .build();

        Topic topic = TopicMapper.requestToTopic(request, group);

        assertSame(group, topic.getGroup());
        assertEquals("airport", topic.getSlug());
        assertEquals("Sân bay", topic.getNameVi());
        assertNull(topic.getNameEn());
        assertNull(topic.getKeywords());
        assertEquals("plane", topic.getIcon());
        assertEquals(CefrLevel.B1, topic.getCefrLevel());
        assertNull(topic.getDescription());
        assertEquals(0, topic.getSortOrder());
        assertTrue(topic.isActive());
    }

    @Test
    void topicToTopicResponse_mapsAllFields() {
        Topic topic = Topic.builder()
                .id(1L)
                .group(group)
                .slug("airport")
                .nameVi("Sân bay")
                .nameEn("Airport")
                .keywords("flight")
                .icon("plane")
                .cefrLevel(CefrLevel.A2)
                .description("desc")
                .sortOrder(2)
                .isActive(false)
                .build();

        TopicResponse response = TopicMapper.topicToTopicResponse(topic);

        assertEquals(1L, response.getId());
        assertEquals(5L, response.getGroupId());
        assertEquals("Du lịch", response.getGroupName());
        assertEquals("airport", response.getSlug());
        assertEquals("Sân bay", response.getNameVi());
        assertEquals("Airport", response.getNameEn());
        assertEquals("flight", response.getKeyword());
        assertEquals("plane", response.getIcon());
        assertEquals(CefrLevel.A2, response.getLevel());
        assertEquals("desc", response.getDescription());
        assertEquals(2, response.getSortBy());
        assertEquals(false, response.getIsActive());
    }

    @Test
    void topicToTopicResponse_nullGroup() {
        Topic topic = Topic.builder().id(1L).slug("airport").nameVi("Sân bay").build();

        TopicResponse response = TopicMapper.topicToTopicResponse(topic);

        assertNull(response.getGroupId());
        assertNull(response.getGroupName());
    }

    @Test
    void updateTopicGroup_onlyNonNullFieldsApplied() {
        TopicGroup target = TopicGroup.builder()
                .id(5L)
                .type(TopicGroupType.SITUATION)
                .name("Du lịch")
                .icon("plane")
                .sortOrder(1)
                .build();

        TopicGroupMapper.updateTopicGroup(
                target,
                UpdateTopicGroupRequest.builder()
                        .name("  Ẩm thực ")
                        .sortOrder(4)
                        .build());

        assertEquals("Ẩm thực", target.getName());
        assertEquals(4, target.getSortOrder());
        assertEquals(TopicGroupType.SITUATION, target.getType());
        assertEquals("plane", target.getIcon());
    }
}
