package backend.dev.PLA.dto.fsrs;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FSRSResult {
    double stability;
    double difficulty;
    int intervalDays;
    LocalDateTime nextReviewAt;
    LocalDateTime lastReviewAt;
}