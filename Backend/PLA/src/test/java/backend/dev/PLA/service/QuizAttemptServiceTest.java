package backend.dev.PLA.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.dev.PLA.dto.request.Quiz.AttemptAnswerRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptRequest;
import backend.dev.PLA.dto.request.Quiz.AttemptSearchRequest;
import backend.dev.PLA.dto.response.PageResponse;
import backend.dev.PLA.dto.response.quiz.AttemptAnswerResponse;
import backend.dev.PLA.dto.response.quiz.AttemptQuestionResponse;
import backend.dev.PLA.dto.response.quiz.AttemptResponse;
import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.dto.response.quiz.QuestionResponse;
import backend.dev.PLA.dto.response.quiz.QuizAttemptHistoryResponse;
import backend.dev.PLA.dto.response.quiz.QuizResponse;
import backend.dev.PLA.entities.quiz.AttemptAnswer;
import backend.dev.PLA.entities.quiz.AttemptAnswerId;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.entities.quiz.Quiz;
import backend.dev.PLA.entities.quiz.QuizAttempt;
import backend.dev.PLA.entities.quiz.Skill;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.enums.QuizStatus;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.quiz.AttemptAnswerRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import backend.dev.PLA.repositories.quiz.QuizRepository;
import backend.dev.PLA.repositories.user.UserRepository;
import backend.dev.PLA.services.quiz.QuizAccessService;
import backend.dev.PLA.services.quiz.QuizAttemptService;
import backend.dev.PLA.services.quiz.QuizService;
import backend.dev.PLA.services.quiz.TopicProgressService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {
    static final long USER_ID = 7L;
    static final long QUIZ_ID = 1L;
    static final Instant NOW = Instant.parse("2026-11-18T08:00:00Z");

    @Mock
    QuizAttemptRepository quizAttemptRepository;

    @Mock
    AttemptAnswerRepository attemptAnswerRepository;

    @Mock
    QuizRepository quizRepository;

    @Mock
    QuestionRepository questionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    QuizAccessService quizAccessService;

    @Mock
    QuizService quizService;

    @Mock
    TopicProgressService topicProgressService;

    @InjectMocks
    QuizAttemptService quizAttemptService;

    Quiz quiz;
    User user;

    // q1 SINGLE_CHOICE (o11 sai, o12 đúng), q2 FILL_BLANK "goes on", q3 SINGLE_CHOICE (o31 đúng)
    Question q1;
    Question q2;
    Question q3;
    Option o11;
    Option o12;
    Option o31;

    @BeforeEach
    void initData() {
        user = User.builder().id(USER_ID).username("userA").build();
        quiz = Quiz.builder()
                .id(QUIZ_ID)
                .title("Quiz A")
                .createdBy(user)
                .status(QuizStatus.READY)
                .skill(Skill.builder().id((short) 2).name("Reading").build())
                .build();

        o11 = Option.builder().id(11L).isCorrect(false).build();
        o12 = Option.builder().id(12L).isCorrect(true).build();
        o31 = Option.builder().id(31L).isCorrect(true).build();
        q1 = question(101L, 1, QuestionType.SINGLE_CHOICE, null, o11, o12);
        q2 = question(102L, 2, QuestionType.FILL_BLANK, "goes on");
        q3 = question(103L, 3, QuestionType.SINGLE_CHOICE, null, o31);
    }

    // ===================== submitAttempt =====================

    @Nested
    class Submit {
        void givenReadyQuiz() {
            when(quizRepository.findByIdForShare(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(questionRepository.findDetailsByQuizId(QUIZ_ID)).thenReturn(List.of(q1, q2, q3));
        }

        void givenSaveWorks() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
            // Giả lập DB: sinh id, started_at (@CreationTimestamp) và khoá ghép của attempt_answers (@MapsId)
            when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
                QuizAttempt saved = inv.getArgument(0);
                saved.setId(50L);
                saved.setStartedAt(NOW);
                saved.getAttemptAnswers()
                        .forEach(a ->
                                a.setId(new AttemptAnswerId(50L, a.getQuestion().getId())));
                return saved;
            });
        }

        @Test
        void gradesAllTypes_savesAndReturnsSortedAnswers() {
            givenReadyQuiz();
            givenSaveWorks();

            // Gửi lộn thứ tự: response phải sắp theo position
            AttemptResponse response = quizAttemptService.submitAttempt(
                    USER_ID,
                    QUIZ_ID,
                    request(
                            answer(q3.getId(), null, null),
                            answer(q2.getId(), null, "  Goes   ON "),
                            answer(q1.getId(), o12.getId(), null)));

            ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
            verify(quizAttemptRepository).save(captor.capture());
            verify(quizAttemptRepository).flush();
            verify(quizAccessService).checkOwner(quiz, USER_ID);

            QuizAttempt saved = captor.getValue();
            assertSame(quiz, saved.getQuiz());
            assertSame(user, saved.getUser());
            assertEquals(3, saved.getTotalQuestions());
            assertEquals(2, saved.getCorrectCount());
            assertEquals(NOW, saved.getSubmittedAt());
            saved.getAttemptAnswers().forEach(a -> assertSame(saved, a.getAttempt()));

            assertEquals(50L, response.getId());
            assertEquals(2, response.getCorrectCount());
            assertEquals(3, response.getTotalQuestions());
            assertEquals(NOW, response.getStartedAt());
            assertEquals(NOW, response.getSubmittedAt());
            assertEquals("Quiz A", response.getQuiz().getName());

            // questions[] gồm mọi câu theo position, mỗi câu kèm options và câu trả lời
            List<AttemptQuestionResponse> questions = response.getQuestions();
            assertEquals(
                    List.of(101L, 102L, 103L),
                    questions.stream().map(q -> q.getQuestion().getId()).toList());
            QuestionResponse first = questions.get(0).getQuestion();
            assertEquals(QUIZ_ID, first.getQuizId());
            assertEquals(
                    List.of(11L, 12L),
                    first.getOptions().stream().map(OptionResponse::getId).toList());
            assertTrue(first.getOptions().get(1).getIsCorrect());
            assertEquals("goes on", questions.get(1).getQuestion().getCorrectText());

            List<AttemptAnswerResponse> answers =
                    questions.stream().map(AttemptQuestionResponse::getAnswer).toList();
            answers.forEach(a -> assertEquals(50L, a.getAttemptId()));
            assertEquals(12L, answers.get(0).getSelectedOptionId());
            assertTrue(answers.get(0).getIsCorrect());
            // answerText lưu bản đã trim, còn so đáp án thì chuẩn hoá hoa thường + khoảng trắng
            assertEquals("Goes   ON", answers.get(1).getAnswerText());
            assertTrue(answers.get(1).getIsCorrect());
            assertNull(answers.get(2).getSelectedOptionId());
            assertNull(answers.get(2).getAnswerText());
            assertFalse(answers.get(2).getIsCorrect());
        }

        @Test
        void updatesTopicProgress_afterSavingAttempt() {
            givenReadyQuiz();
            givenSaveWorks();

            quizAttemptService.submitAttempt(
                    USER_ID,
                    QUIZ_ID,
                    request(answer(q1.getId(), o12.getId(), null), answer(q2.getId(), null, "goes on")));

            // Gọi sau khi đã lưu attempt, với user nộp bài và số câu đúng / tổng số câu của quiz
            var order = inOrder(quizAttemptRepository, topicProgressService);
            order.verify(quizAttemptRepository).flush();
            order.verify(topicProgressService).recordAttempt(USER_ID, quiz, 2, 3, NOW);
        }

        @Test
        void wrongOptionAndWrongText_areIncorrect() {
            givenReadyQuiz();
            givenSaveWorks();

            AttemptResponse response = quizAttemptService.submitAttempt(
                    USER_ID,
                    QUIZ_ID,
                    request(answer(q1.getId(), o11.getId(), null), answer(q2.getId(), null, "go on")));

            assertEquals(0, response.getCorrectCount());
            assertEquals(3, response.getTotalQuestions());
            assertEquals(3, response.getQuestions().size());
            assertFalse(response.getQuestions().get(0).getAnswer().getIsCorrect());
            assertFalse(response.getQuestions().get(1).getAnswer().getIsCorrect());
            // q3 không gửi lên → answer = null
            assertNull(response.getQuestions().get(2).getAnswer());
        }

        @Test
        void blankAnswerText_isTreatedAsSkipped() {
            givenReadyQuiz();
            givenSaveWorks();

            // "   " trên câu SINGLE_CHOICE không bị coi là gửi answerText (không lỗi mismatch)
            AttemptResponse response = quizAttemptService.submitAttempt(
                    USER_ID, QUIZ_ID, request(answer(q1.getId(), null, "   "), answer(q2.getId(), null, "  ")));

            assertEquals(0, response.getCorrectCount());
            response.getQuestions().stream()
                    .map(AttemptQuestionResponse::getAnswer)
                    .limit(2)
                    .forEach(a -> {
                        assertNull(a.getAnswerText());
                        assertFalse(a.getIsCorrect());
                    });
            assertNull(response.getQuestions().get(2).getAnswer());
        }

        @Test
        void fillBlankWithoutCorrectText_isIncorrect() {
            q2.setCorrectText(null);
            givenReadyQuiz();
            givenSaveWorks();

            AttemptResponse response =
                    quizAttemptService.submitAttempt(USER_ID, QUIZ_ID, request(answer(q2.getId(), null, "goes on")));

            assertEquals(0, response.getCorrectCount());
        }

        @Test
        void quizNotFound() {
            when(quizRepository.findByIdForShare(QUIZ_ID)).thenReturn(Optional.empty());

            expectError(ErrorCode.QUIZ_NOT_FOUND, request(answer(q1.getId(), o12.getId(), null)));
            verify(questionRepository, never()).findDetailsByQuizId(any());
        }

        @Test
        void notOwner() {
            when(quizRepository.findByIdForShare(QUIZ_ID)).thenReturn(Optional.of(quiz));
            doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                    .when(quizAccessService)
                    .checkOwner(quiz, USER_ID);

            expectError(ErrorCode.ACCESS_DENIED, request(answer(q1.getId(), o12.getId(), null)));
        }

        @ParameterizedTest
        @EnumSource(value = QuizStatus.class, names = "READY", mode = EnumSource.Mode.EXCLUDE)
        void quizNotReady(QuizStatus status) {
            quiz.setStatus(status);
            when(quizRepository.findByIdForShare(QUIZ_ID)).thenReturn(Optional.of(quiz));

            expectError(ErrorCode.QUIZ_NOT_READY, request(answer(q1.getId(), o12.getId(), null)));
            verify(questionRepository, never()).findDetailsByQuizId(any());
        }

        @Test
        void quizWithoutQuestions() {
            when(quizRepository.findByIdForShare(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(questionRepository.findDetailsByQuizId(QUIZ_ID)).thenReturn(List.of());

            expectError(ErrorCode.QUIZ_HAS_NO_QUESTIONS, request(answer(q1.getId(), o12.getId(), null)));
        }

        @Test
        void questionNotInQuiz() {
            givenReadyQuiz();
            expectError(ErrorCode.ATTEMPT_QUESTION_NOT_IN_QUIZ, request(answer(999L, null, "goes")));
        }

        @Test
        void duplicatedQuestion() {
            givenReadyQuiz();
            expectError(
                    ErrorCode.ATTEMPT_QUESTION_DUPLICATED,
                    request(answer(q1.getId(), o12.getId(), null), answer(q1.getId(), o11.getId(), null)));
        }

        @Test
        void optionOfOtherQuestion() {
            givenReadyQuiz();
            expectError(ErrorCode.ATTEMPT_OPTION_NOT_IN_QUESTION, request(answer(q1.getId(), o31.getId(), null)));
        }

        @Test
        void optionOnQuestionWithoutOptions() {
            q3.setOptions(null);
            givenReadyQuiz();
            expectError(ErrorCode.ATTEMPT_OPTION_NOT_IN_QUESTION, request(answer(q3.getId(), o31.getId(), null)));
        }

        @Test
        void singleChoiceWithAnswerText() {
            givenReadyQuiz();
            expectError(ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH, request(answer(q1.getId(), null, "goes")));
        }

        @Test
        void fillBlankWithOptionId() {
            givenReadyQuiz();
            expectError(ErrorCode.ATTEMPT_ANSWER_TYPE_MISMATCH, request(answer(q2.getId(), o12.getId(), null)));
        }

        @Test
        void errorAfterValidAnswers_savesNothing() {
            givenReadyQuiz();
            // Câu đầu hợp lệ, câu sau lỗi: cả lượt bị huỷ
            expectError(
                    ErrorCode.ATTEMPT_QUESTION_NOT_IN_QUIZ,
                    request(answer(q1.getId(), o12.getId(), null), answer(999L, null, null)));
        }

        void expectError(ErrorCode errorCode, AttemptRequest request) {
            AppException ex =
                    assertThrows(AppException.class, () -> quizAttemptService.submitAttempt(USER_ID, QUIZ_ID, request));
            assertEquals(errorCode, ex.getErrorCode());
            verify(quizAttemptRepository, never()).save(any());
            verify(topicProgressService, never()).recordAttempt(any(), any(), anyInt(), anyInt(), any());
        }
    }

    // ===================== getMyAttempts =====================

    @Nested
    class MyAttempts {
        @Test
        void pagesNewestFirst_withoutAnswers() {
            QuizAttempt attempt = attempt(5L);
            when(quizAttemptRepository.findByUserId(any(), any()))
                    .thenAnswer(inv -> new PageImpl<>(List.of(attempt), inv.getArgument(1), 3));

            PageResponse<AttemptResponse> page = quizAttemptService.getMyAttempts(
                    USER_ID, AttemptSearchRequest.builder().page(1).size(2).build());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(quizAttemptRepository).findByUserId(org.mockito.ArgumentMatchers.eq(USER_ID), pageable.capture());
            assertEquals(1, pageable.getValue().getPageNumber());
            assertEquals(2, pageable.getValue().getPageSize());
            assertEquals(
                    Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id")),
                    pageable.getValue().getSort());

            assertEquals(1, page.getPage());
            assertEquals(2, page.getSize());
            assertEquals(3, page.getTotalElements());
            assertEquals(2, page.getTotalPages());
            assertEquals(1, page.getContent().size());
            assertEquals(5L, page.getContent().get(0).getId());
            assertEquals("Reading", page.getContent().get(0).getSkill().getName());
            assertNull(page.getContent().get(0).getQuestions());
        }

        @Test
        void empty() {
            Page<QuizAttempt> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(quizAttemptRepository.findByUserId(any(), any())).thenReturn(empty);

            PageResponse<AttemptResponse> page = quizAttemptService.getMyAttempts(USER_ID, new AttemptSearchRequest());

            assertTrue(page.getContent().isEmpty());
            assertEquals(0, page.getTotalElements());
        }
    }

    // ===================== getQuizAttempts =====================

    @Nested
    class QuizAttempts {
        QuizResponse quizResponse;

        @BeforeEach
        void initQuizResponse() {
            quizResponse = QuizResponse.builder()
                    .id(QUIZ_ID)
                    .questions(List.of(
                            QuestionResponse.builder().id(101L).build(),
                            QuestionResponse.builder().id(102L).build()))
                    .build();
        }

        @Test
        void groupsAnswersByAttempt() {
            when(quizService.getQuizDetail(QUIZ_ID)).thenReturn(quizResponse);
            when(quizAttemptRepository.findByQuizIdAndUserIdOrderBySubmittedAtDescIdDesc(QUIZ_ID, USER_ID))
                    .thenReturn(List.of(attempt(9L), attempt(8L)));
            when(attemptAnswerRepository.findByAttemptIds(anyCollection()))
                    .thenReturn(List.of(
                            savedAnswer(9L, 101L, 12L, null, true),
                            savedAnswer(8L, 101L, 11L, null, false),
                            savedAnswer(9L, 102L, null, "goes on", true)));

            QuizAttemptHistoryResponse response = quizAttemptService.getQuizAttempts(USER_ID, QUIZ_ID);

            verify(attemptAnswerRepository).findByAttemptIds(List.of(9L, 8L));
            assertSame(quizResponse, response.getQuiz());
            assertEquals(2, response.getAttempts().size());

            AttemptResponse newest = response.getAttempts().get(0);
            assertEquals(9L, newest.getId());
            assertEquals(2, newest.getQuestions().size());
            assertEquals(12L, newest.getQuestions().get(0).getAnswer().getSelectedOptionId());
            assertEquals("goes on", newest.getQuestions().get(1).getAnswer().getAnswerText());

            AttemptResponse older = response.getAttempts().get(1);
            assertEquals(8L, older.getId());
            assertFalse(older.getQuestions().get(0).getAnswer().getIsCorrect());
            assertNull(older.getQuestions().get(1).getAnswer());
        }

        @Test
        void noAttempts_throwsQuizNoAttempts_withoutLoadingAnswers() {
            when(quizAttemptRepository.findByQuizIdAndUserIdOrderBySubmittedAtDescIdDesc(QUIZ_ID, USER_ID))
                    .thenReturn(List.of());

            AppException ex =
                    assertThrows(AppException.class, () -> quizAttemptService.getQuizAttempts(USER_ID, QUIZ_ID));
            assertEquals(ErrorCode.QUIZ_NO_ATTEMPTS, ex.getErrorCode());
            // Không trả đề kèm đáp án, không đọc câu trả lời
            verify(quizService, never()).getQuizDetail(any());
            verify(attemptAnswerRepository, never()).findByAttemptIds(anyCollection());
        }

        @Test
        void quizErrorsArePropagated() {
            // 404 / 403 do QuizAccessService ném: không lấy đề kèm đáp án, không đọc lượt làm
            when(quizAccessService.findViewableQuiz(USER_ID, QUIZ_ID))
                    .thenThrow(new AppException(ErrorCode.ACCESS_DENIED));

            AppException ex =
                    assertThrows(AppException.class, () -> quizAttemptService.getQuizAttempts(USER_ID, QUIZ_ID));
            assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
            verify(quizAttemptRepository, never()).findByQuizIdAndUserIdOrderBySubmittedAtDescIdDesc(any(), any());
        }
    }

    // ===================== Helpers =====================

    Question question(long id, int position, QuestionType type, String correctText, Option... options) {
        Question question = Question.builder()
                .id(id)
                .quiz(quiz)
                .position((short) position)
                .type(type)
                .content("Question " + id)
                .correctText(correctText)
                .options(Arrays.asList(options))
                .build();
        for (int i = 0; i < options.length; i++) {
            options[i].setQuestion(question);
            options[i].setPosition((short) (i + 1));
        }
        return question;
    }

    static AttemptAnswerRequest answer(Long questionId, Long optionId, String answerText) {
        return AttemptAnswerRequest.builder()
                .questionId(questionId)
                .optionId(optionId)
                .answerText(answerText)
                .build();
    }

    static AttemptRequest request(AttemptAnswerRequest... answers) {
        return AttemptRequest.builder().answers(List.of(answers)).build();
    }

    QuizAttempt attempt(long id) {
        return QuizAttempt.builder()
                .id(id)
                .quiz(quiz)
                .user(user)
                .startedAt(NOW)
                .submittedAt(NOW)
                .correctCount(1)
                .totalQuestions(2)
                .build();
    }

    static AttemptAnswer savedAnswer(long attemptId, long questionId, Long optionId, String text, boolean correct) {
        return AttemptAnswer.builder()
                .id(new AttemptAnswerId(attemptId, questionId))
                .selectedOption(
                        optionId == null ? null : Option.builder().id(optionId).build())
                .answerText(text)
                .isCorrect(correct)
                .build();
    }
}
