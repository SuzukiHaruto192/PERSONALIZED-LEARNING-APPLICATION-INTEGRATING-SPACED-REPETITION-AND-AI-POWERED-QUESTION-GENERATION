package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.quiz.DueTopicProgressResponse;
import backend.dev.PLA.dto.response.quiz.ProgressTopicResponse;
import backend.dev.PLA.dto.response.quiz.RefResponse;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.UserTopicProgress;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TopicProgressMapper {

    /** Chỉ gọi cho dòng đã đến hạn (next_review khác null và <= today). */
    public static DueTopicProgressResponse toDueResponse(UserTopicProgress progress, LocalDate today) {
        Skill skill = progress.getSkill();
        Topic topic = progress.getTopic();
        return DueTopicProgressResponse.builder()
                .skill(new RefResponse(skill.getId().longValue(), skill.getName()))
                .topic(ProgressTopicResponse.builder()
                        .id(topic.getId())
                        .slug(topic.getSlug())
                        .nameVi(topic.getNameVi())
                        .nameEn(topic.getNameEn())
                        .cefrLevel(topic.getCefrLevel())
                        .build())
                .overdueDays(ChronoUnit.DAYS.between(progress.getNextReviewDate(), today))
                .build();
    }
}
