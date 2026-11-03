package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.entities.FlashCardDeck;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FlashCardDeckMapper {
    FlashCardDeckResponse modelToResponse(FlashCardDeck deck);
    List<FlashCardDeckResponse> modelsToResponses(List<FlashCardDeck> decks);
}
