package backend.dev.PLA.services;

import backend.dev.PLA.dto.fsrs.FSRSResult;
import backend.dev.PLA.dto.request.FlashCardRequest;
import backend.dev.PLA.dto.request.NewFlashCardRequest;
import backend.dev.PLA.dto.response.FlashCardResponse;
import backend.dev.PLA.entities.FlashCard;
import backend.dev.PLA.entities.FlashCardDetail;
import backend.dev.PLA.entities.FlashCardDetailId;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.FlashCardMapper;
import backend.dev.PLA.repositories.FlashCardDetailRepository;
import backend.dev.PLA.repositories.FlashCardRepository;
import backend.dev.PLA.repositories.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardService {
    FlashCardRepository flashCardRepository;
    FlashCardMapper flashCardMapper;
    UserRepository userRepository;
    FlashCardDetailRepository flashCardDetailRepository;
    SpacedRepetitionService spacedRepetitionService;

    public List<FlashCardResponse> getMyFlashCard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<FlashCard> flashCards = flashCardRepository.findAllByUsername(authentication.getName());
        return flashCardMapper.modelsToResponses(flashCards);
    }

    @Transactional
    public void createFlashCard(NewFlashCardRequest newflashCardRequest) {
        CustomUserDetail userDetails = (CustomUserDetail) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(userDetails.getId());

        String front = newflashCardRequest.getFrontContent().trim();
        String back = newflashCardRequest.getBackContent().trim();

        FlashCard flashCard = flashCardRepository.findByContent(front, back);

        if (flashCard == null) {
            flashCard = flashCardMapper.requestToModel(newflashCardRequest);
            flashCard.setFrontContent(front);
            flashCard.setBackContent(back);
            flashCard = flashCardRepository.save(flashCard);
        } else {
            FlashCardDetailId checkId = new FlashCardDetailId(user.getId(), flashCard.getId());
            if (flashCardDetailRepository.existsById(checkId)) {
                throw new AppException(ErrorCode.FLASHCARD_DETAIL_EXISTED);
            }
        }

        FlashCardDetailId detailId = new FlashCardDetailId(user.getId(), flashCard.getId());
        FlashCardDetail detail = getFlashCardDetail(flashCard , detailId , user ,0.0 , 0.0 , null , null , 0 , false );

        flashCardDetailRepository.save(detail);
    }

    @Transactional
    public FSRSResult reviewFlashCard(FlashCardRequest request) {
        CustomUserDetail userDetails = (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        Long flashCardId = request.getFlashCardId();

        FlashCardDetailId detailId = new FlashCardDetailId(userId, flashCardId);
        FlashCardDetail detail = flashCardDetailRepository.findById(detailId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_NOT_FOUND));
        
        FSRSResult result = spacedRepetitionService.calculateNextReview(
                detail.getStability() , detail.getDifficulty() ,detail.getNextReviewDate(), request.getRating()
        );

        FlashCardDetail updatedDetail = getFlashCardDetail(
                detail.getFlashCard() ,
                detail.getId() ,
                detail.getUser(),
                result.getStability() ,
                result.getDifficulty(),
                result.getNextReviewAt() ,
                result.getLastReviewAt() ,
                detail.getLapses()+1,
                true
        );

        flashCardDetailRepository.save(detail);

        return result;
    }
    private FlashCardDetail getFlashCardDetail(
            FlashCard flashCard ,
            FlashCardDetailId detailId ,
            User user ,
            Double stability,
            Double difficulty ,
            LocalDateTime nextReviewDate ,
            LocalDateTime lastReviewDate  ,
            Integer lapses ,
            boolean state) {
        return FlashCardDetail.builder()
                .id(detailId)
                .flashCard(flashCard)
                .user(user)
                .stability(stability)
                .difficulty(difficulty)
                .nextReviewDate(nextReviewDate)
                .lastReviewDate(lastReviewDate)
                .lapses(lapses)
                .state(state)
                .build();
    }
}
