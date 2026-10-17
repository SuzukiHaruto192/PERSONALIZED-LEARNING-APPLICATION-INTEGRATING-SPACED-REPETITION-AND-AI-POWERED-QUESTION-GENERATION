package backend.dev.PLA.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Rating {
    NEW(0),
    AGAIN(1),
    HARD(2),
    NORMAL(3),
    EASY(4);

    @JsonValue
    private final int number;

    public boolean isValidFSRSRating() {
        return this.number >= 1 && this.number <= 4;
    }

    @JsonCreator
    public static Rating fromNumber(int number) {
        return Arrays.stream(Rating.values())
                .filter(r -> r.getNumber() == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mã Rating không hợp lệ: " + number));
    }
}