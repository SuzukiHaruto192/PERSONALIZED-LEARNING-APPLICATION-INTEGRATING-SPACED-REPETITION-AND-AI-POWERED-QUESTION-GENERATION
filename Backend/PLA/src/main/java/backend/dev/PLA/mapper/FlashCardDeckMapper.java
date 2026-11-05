package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.flashcard.FlashCardDeckResponse;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlashCardDeckMapper {
    FlashCardDeckResponse modelToResponse(FlashCardDeck deck);

    List<FlashCardDeckResponse> modelsToResponses(List<FlashCardDeck> decks);
}
