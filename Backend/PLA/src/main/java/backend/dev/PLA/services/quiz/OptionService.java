package backend.dev.PLA.services.quiz;

import backend.dev.PLA.dto.request.Quiz.OptionRequest;
import backend.dev.PLA.dto.response.quiz.OptionResponse;
import backend.dev.PLA.entities.quiz.Option;
import backend.dev.PLA.entities.quiz.Question;
import backend.dev.PLA.enums.QuestionType;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.OptionMapper;
import backend.dev.PLA.mapper.QuestionMapper;
import backend.dev.PLA.repositories.quiz.OptionRepository;
import backend.dev.PLA.repositories.quiz.QuestionRepository;
import backend.dev.PLA.repositories.quiz.QuizAttemptRepository;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OptionService {
    static final String HAS_ATTEMPTS_MESSAGE =
            "Quiz đã có lượt làm bài, không thể thêm, xoá đáp án hoặc đổi đáp án đúng";

    OptionRepository optionRepository;
    QuestionRepository questionRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizAccessService quizAccessService;

    // Mọi thao tác ghi đều khoá dòng quiz cha (findWritableQuiz) nên đếm số đáp án / tính max + 1 / kiểm tra trùng
    // position / đổi đáp án đúng không bị request song song chen vào

    @Transactional(rollbackFor = Exception.class)
    public OptionResponse createOption(Long userId, Long questionId, OptionRequest request) {
        Question question = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        Long quizId = question.getQuiz().getId();
        quizAccessService.findWritableQuiz(userId, quizId);

        if (question.getType() != QuestionType.SINGLE_CHOICE) {
            throw new AppException(ErrorCode.OPTION_NOT_ALLOWED);
        }
        // attempt_answers.selected_option_id trỏ tới options: không thêm / xoá / đổi đáp án đúng khi đã có lượt làm
        if (quizAttemptRepository.existsByQuizId(quizId)) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS, HAS_ATTEMPTS_MESSAGE);
        }
        if (optionRepository.countByQuestionId(questionId) >= QuestionMapper.MAX_OPTIONS) {
            throw new AppException(ErrorCode.QUESTION_OPTION_LIMIT);
        }

        short position;
        if (request.getPosition() == null) {
            int max = optionRepository.findMaxPositionByQuestionId(questionId);
            if (max >= Short.MAX_VALUE) {
                throw new AppException(ErrorCode.OPTION_POSITION_INVALID);
            }
            position = (short) (max + 1);
        } else {
            position = request.getPosition().shortValue();
            if (optionRepository.existsByQuestionIdAndPosition(questionId, position)) {
                throw new AppException(ErrorCode.OPTION_POSITION_DUPLICATED);
            }
        }

        // uq_options_one_correct không deferrable: gỡ đáp án đúng cũ (và flush) trước khi lưu đáp án mới
        if (Boolean.TRUE.equals(request.getIsCorrect())) {
            optionRepository.unsetCorrectByQuestionId(questionId);
        }

        Option option = Option.builder()
                .question(questionRepository.getReferenceById(questionId))
                .position(position)
                .build();
        OptionMapper.updateOption(option, request);
        return OptionMapper.toOptionResponse(optionRepository.saveAndFlush(option));
    }

    @Transactional(readOnly = true)
    public List<OptionResponse> getOptions(Long userId, Long questionId) {
        Question question = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        quizAccessService.checkOwner(question.getQuiz(), userId);
        return optionRepository.findByQuestionIdOrderByPositionAsc(questionId).stream()
                .map(OptionMapper::toOptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OptionResponse getOption(Long userId, Long optionId) {
        Option option = findOption(optionId);
        quizAccessService.checkOwner(option.getQuestion().getQuiz(), userId);
        return OptionMapper.toOptionResponse(option);
    }

    @Transactional(rollbackFor = Exception.class)
    public OptionResponse updateOption(Long userId, Long optionId, OptionRequest request) {
        Option option = findOption(optionId);
        Long questionId = option.getQuestion().getId();
        Long quizId = option.getQuestion().getQuiz().getId();
        quizAccessService.findWritableQuiz(userId, quizId);

        boolean newCorrect = Boolean.TRUE.equals(request.getIsCorrect());
        if (newCorrect != option.isCorrect() && quizAttemptRepository.existsByQuizId(quizId)) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS, HAS_ATTEMPTS_MESSAGE);
        }

        // position bỏ trống thì giữ nguyên vị trí cũ
        Short position = option.getPosition();
        if (request.getPosition() != null) {
            position = request.getPosition().shortValue();
            if (optionRepository.existsByQuestionIdAndPositionAndIdNot(questionId, position, optionId)) {
                throw new AppException(ErrorCode.OPTION_POSITION_DUPLICATED);
            }
        }

        // Gỡ đáp án đúng khác (và flush) trước khi đặt đáp án này là đúng. Câu UPDATE xoá persistence context
        // nên phải nạp lại option
        if (newCorrect && !option.isCorrect()) {
            optionRepository.unsetCorrectByQuestionIdAndIdNot(questionId, optionId);
            option = findOption(optionId);
        }

        option.setPosition(position);
        OptionMapper.updateOption(option, request);
        return OptionMapper.toOptionResponse(optionRepository.saveAndFlush(option));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOption(Long userId, Long optionId) {
        Option option = findOption(optionId);
        Long quizId = option.getQuestion().getQuiz().getId();
        quizAccessService.findWritableQuiz(userId, quizId);
        // attempt_answers.selected_option_id không có ON DELETE: chặn trước khi database báo lỗi khoá ngoại
        if (quizAttemptRepository.existsByQuizId(quizId)) {
            throw new AppException(ErrorCode.QUIZ_HAS_ATTEMPTS, HAS_ATTEMPTS_MESSAGE);
        }
        // Không đánh lại position các đáp án còn lại
        optionRepository.deleteByIdDirectly(optionId);
    }

    private Option findOption(Long optionId) {
        return optionRepository.findById(optionId).orElseThrow(() -> new AppException(ErrorCode.OPTION_NOT_FOUND));
    }
}
