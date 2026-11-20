package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.flashcard.DeckFlashCardResponse;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlashCardMapper {
    @Mapping(target = "sourcePassageId", source = "sourcePassage.id")
    FlashCardResponse modelToResponse(FlashCard card);

    List<FlashCardResponse> modelsToResponses(List<FlashCard> cards);

    /** Thẻ trong deck kèm lịch ôn; {@code today} dùng để tính số ngày quá hạn. */
    default DeckFlashCardResponse detailToResponse(FlashCardDetail detail, LocalDate today) {
        FlashCard card = detail.getFlashCard();
        LocalDate next = detail.getNextReviewDate();
        long overdueDays = next != null && next.isBefore(today) ? ChronoUnit.DAYS.between(next, today) : 0;
        return DeckFlashCardResponse.builder()
                .deckId(detail.getDeck().getId())
                .deckName(detail.getDeck().getName())
                .flashCardId(card.getId())
                .frontContent(card.getFrontContent())
                .backContent(card.getBackContent())
                .example(card.getExample())
                .sourcePassageId(
                        card.getSourcePassage() != null
                                ? card.getSourcePassage().getId()
                                : null)
                .fsrsState(detail.getFsrsState())
                .lapses(detail.getLapses())
                .lastReviewDate(detail.getLastReviewDate())
                .nextReviewDate(next)
                .overdueDays(overdueDays)
                .addedAt(detail.getAddedAt())
                .build();
    }
}
