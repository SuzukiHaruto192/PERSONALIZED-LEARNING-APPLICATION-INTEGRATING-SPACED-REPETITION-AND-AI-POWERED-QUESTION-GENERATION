package backend.dev.PLA.repository;

import static org.junit.jupiter.api.Assertions.*;

import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop") // application-dev.yml để none nên DB test
// không có bảng
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FlashCardRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine"); // cùng image với docker-compose

    @Autowired
    private FlashCardRepository flashCardRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllByUsername_shouldReturnOnlyCardsOfThatUser() {
        User quang = persistUser("quang");
        User minh = persistUser("minh");
        FlashCard cardOfQuang = persistCard("apple", "qua tao");
        FlashCard cardOfMinh = persistCard("book", "quyen sach");
        persistDetail(persistDeck(quang, "deck"), cardOfQuang);
        persistDetail(persistDeck(minh, "deck"), cardOfMinh);
        entityManager.clear();

        List<FlashCard> result = flashCardRepository.findAllByUsername("quang");

        assertEquals(1, result.size());
        assertEquals(cardOfQuang.getId(), result.getFirst().getId());
    }

    @Test
    void findAllByUsername_shouldReturnSharedCardForEachUser() {
        User quang = persistUser("quang");
        User minh = persistUser("minh");
        FlashCard shared = persistCard("apple", "qua tao");
        persistDetail(persistDeck(quang, "deck"), shared);
        persistDetail(persistDeck(minh, "deck"), shared);
        entityManager.clear();

        assertEquals(1, flashCardRepository.findAllByUsername("quang").size());
        assertEquals(1, flashCardRepository.findAllByUsername("minh").size());
    }

    @Test
    void findAllByUsername_shouldNotDuplicateCardInTwoDecksOfSameUser() {
        User quang = persistUser("quang");
        FlashCard card = persistCard("apple", "qua tao");
        persistDetail(persistDeck(quang, "deck 1"), card);
        persistDetail(persistDeck(quang, "deck 2"), card);
        entityManager.clear();

        assertEquals(1, flashCardRepository.findAllByUsername("quang").size());
    }

    @Test
    void findAllByDeckId_shouldReturnOnlyCardsOfThatDeck() {
        User quang = persistUser("quang");
        FlashCardDeck deck1 = persistDeck(quang, "deck 1");
        FlashCardDeck deck2 = persistDeck(quang, "deck 2");
        FlashCard card1 = persistCard("apple", "qua tao");
        persistDetail(deck1, card1);
        persistDetail(deck2, persistCard("book", "quyen sach"));
        entityManager.clear();

        List<FlashCard> result = flashCardRepository.findAllByDeckId(deck1.getId());

        assertEquals(1, result.size());
        assertEquals(card1.getId(), result.getFirst().getId());
    }

    @Test
    void findAllByUsername_shouldReturnEmpty_whenUserHasNoCards() {
        persistUser("quang");
        persistCard("apple", "qua tao");
        entityManager.clear();

        assertTrue(flashCardRepository.findAllByUsername("quang").isEmpty());
    }

    @Test
    void findAllByUsername_shouldReturnEmpty_whenUsernameDoesNotExist() {
        assertTrue(flashCardRepository.findAllByUsername("khong-ton-tai").isEmpty());
    }

    @Test
    void findByContent_shouldIgnoreCase() {
        FlashCard card = persistCard("Apple", "Qua Tao");
        entityManager.clear();

        FlashCard result = flashCardRepository.findByContent("aPPLE", "qua tao");

        assertNotNull(result);
        assertEquals(card.getId(), result.getId());
    }

    @Test
    void findByContent_shouldReturnNull_whenOnlyFrontMatches() {
        persistCard("apple", "qua tao");
        entityManager.clear();

        assertNull(flashCardRepository.findByContent("apple", "quyen sach"));
    }

    @Test
    void findByContent_shouldReturnNull_whenOnlyBackMatches() {
        persistCard("apple", "qua tao");
        entityManager.clear();

        assertNull(flashCardRepository.findByContent("book", "qua tao"));
    }

    @Test
    void findByContent_shouldReturnOneCard_whenDuplicatesExist() {
        persistCard("apple", "qua tao");
        persistCard("APPLE", "QUA TAO");
        entityManager.clear();

        // kiểu trả về là một FlashCard nên thiếu LIMIT 1 sẽ ném lỗi non-unique result
        assertNotNull(flashCardRepository.findByContent("apple", "qua tao"));
    }

    private User persistUser(String username) {
        return entityManager.persistAndFlush(User.builder()
                .username(username)
                .password("encoded")
                .email(username + "@gmail.com")
                .phoneNumber("070" + Math.abs(username.hashCode() % 10000000))
                .build());
    }

    private FlashCard persistCard(String front, String back) {
        return entityManager.persistAndFlush(
                FlashCard.builder().frontContent(front).backContent(back).build());
    }

    private FlashCardDeck persistDeck(User user, String name) {
        return entityManager.persistAndFlush(
                FlashCardDeck.builder().user(user).name(name).build());
    }

    private void persistDetail(FlashCardDeck deck, FlashCard flashCard) {
        entityManager.persistAndFlush(
                FlashCardDetail.builder().deck(deck).flashCard(flashCard).build());
    }
}
