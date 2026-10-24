package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.flashcard.NewFlashCardRequest;
import backend.dev.PLA.dto.response.flashcard.FlashCardResponse;
import backend.dev.PLA.entities.FlashCard;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FlashCardMapper {
    public FlashCardResponse modelToResponse(FlashCard card);
    public List<FlashCardResponse> modelsToResponses(List<FlashCard> cards);
    public FlashCard requestToModel(NewFlashCardRequest request);
}
