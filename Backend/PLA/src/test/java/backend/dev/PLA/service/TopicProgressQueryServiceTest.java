package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.Quiz.DueProgressSearchRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.DueTopicProgressResponse;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.quiz.Topic;
import backend.dev.PLA.entities.quiz.UserTopicProgress;
import backend.dev.PLA.entities.quiz.UserTopicProgressId;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.repositories.quiz.UserTopicProgressRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.quiz.TopicProgressQueryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TopicProgressQueryServiceTest {
    static final long USER_ID = 7L;
    static final short READING = 2;

    @Mock
    UserTopicProgressRepository repository;

    TopicProgressQueryService service;
    CustomUserDetail userDetail;

    @BeforeEach
    void init() {
        service = new TopicProgressQueryService(repository);
        userDetail = new CustomUserDetail(
                User.builder().id(USER_ID).username("u").password("x").build());
    }

    @Test
    void getDueProgress_mapsPageAndOverdueDays() {
        LocalDate today = LocalDate.now();
        when(repository.findDue(eq(USER_ID), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(progress(12L, today.minusDays(3)), progress(13L, today)), PageRequest.of(1, 5), 7));

        PageResponse<DueTopicProgressResponse> page = service.getDueProgress(
                userDetail, DueProgressSearchRequest.builder().page(1).size(5).build());

        assertEquals(7L, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getPage());
        DueTopicProgressResponse first = page.getContent().getFirst();
        assertEquals(2L, first.getSkill().getId());
        assertEquals("Reading", first.getSkill().getName());
        assertEquals(12L, first.getTopic().getId());
        assertEquals("travel", first.getTopic().getSlug());
        assertEquals(3L, first.getOverdueDays());
        assertEquals(0L, page.getContent().get(1).getOverdueDays());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findDue(eq(USER_ID), eq(today), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
    }

    @Test
    void getDueProgress_nothingDue_returnsEmptyPage() {
        when(repository.findDue(eq(USER_ID), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        PageResponse<DueTopicProgressResponse> page =
                service.getDueProgress(userDetail, new DueProgressSearchRequest());

        assertEquals(0L, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    private UserTopicProgress progress(Long topicId, LocalDate nextReview) {
        Skill skill =
                Skill.builder().id(READING).code("READING").name("Reading").build();
        Topic topic =
                Topic.builder().id(topicId).slug("travel").nameVi("Du lịch").build();
        return UserTopicProgress.builder()
                .id(new UserTopicProgressId(USER_ID, READING, topicId))
                .skill(skill)
                .topic(topic)
                .nextReviewDate(nextReview)
                .build();
    }
}
