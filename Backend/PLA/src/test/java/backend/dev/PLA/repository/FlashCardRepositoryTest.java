package backend.dev.PLA.repository;

import static org.junit.jupiter.api.Assertions.*;

import backend.dev.PLA.entities.flashcard.FlashCard;
import backend.dev.PLA.entities.flashcard.FlashCardDeck;
import backend.dev.PLA.entities.flashcard.FlashCardDetail;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.repositories.flashcard.FlashCardRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest // schema do Flyway tạo (db/migration) trên container Postgres
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
    void findByOwnerAndContent_shouldIgnoreCase() {
        User quang = persistUser("quang");
        FlashCard card = persistCard(quang, "Apple", "Qua Tao");
        entityManager.clear();

        Optional<FlashCard> result = flashCardRepository.findByOwnerAndContent(quang.getId(), "aPPLE", "qua tao");

        assertTrue(result.isPresent());
        assertEquals(card.getId(), result.get().getId());
    }

    @Test
    void findByOwnerAndContent_shouldNotReuseCardOfAnotherUser() {
        User quang = persistUser("quang");
        User minh = persistUser("minh");
        persistCard(minh, "apple", "qua tao");
        entityManager.clear();

        assertTrue(flashCardRepository
                .findByOwnerAndContent(quang.getId(), "apple", "qua tao")
                .isEmpty());
    }

    @Test
    void findByOwnerAndContent_shouldBeEmpty_whenOnlyFrontOrBackMatches() {
        User quang = persistUser("quang");
        persistCard(quang, "apple", "qua tao");
        entityManager.clear();

        assertTrue(flashCardRepository
                .findByOwnerAndContent(quang.getId(), "apple", "quyen sach")
                .isEmpty());
        assertTrue(flashCardRepository
                .findByOwnerAndContent(quang.getId(), "book", "qua tao")
                .isEmpty());
    }

    @Test
    void findByOwnerAndContent_shouldReturnOneCard_whenDuplicatesExist() {
        User quang = persistUser("quang");
        persistCard(quang, "apple", "qua tao");
        persistCard(quang, "APPLE", "QUA TAO");
        entityManager.clear();

        // kiểu trả về là một FlashCard nên thiếu LIMIT 1 sẽ ném lỗi non-unique result
        assertTrue(flashCardRepository
                .findByOwnerAndContent(quang.getId(), "apple", "qua tao")
                .isPresent());
    }

    @Test
    void existsOtherInUserDecksByContent_shouldIgnoreCardItselfAndOtherUsers() {
        User quang = persistUser("quang");
        User minh = persistUser("minh");
        FlashCard mine = persistCard(quang, "apple", "qua tao");
        persistDetail(persistDeck(quang, "deck"), mine);
        persistDetail(persistDeck(minh, "deck"), persistCard(minh, "book", "quyen sach"));
        entityManager.clear();

        assertFalse(
                flashCardRepository.existsOtherInUserDecksByContent(quang.getId(), "APPLE", "qua tao", mine.getId()));
        assertFalse(
                flashCardRepository.existsOtherInUserDecksByContent(quang.getId(), "book", "quyen sach", mine.getId()));
        assertTrue(
                flashCardRepository.existsOtherInUserDecksByContent(minh.getId(), "book", "quyen sach", mine.getId()));
    }

    @Test
    void deleteOrphansIn_shouldOnlyDeleteCardsWithoutDetail() {
        User quang = persistUser("quang");
        FlashCard orphan = persistCard(quang, "apple", "qua tao");
        FlashCard used = persistCard(quang, "book", "quyen sach");
        persistDetail(persistDeck(quang, "deck"), used);
        entityManager.clear();

        int deleted = flashCardRepository.deleteOrphansIn(List.of(orphan.getId(), used.getId()));

        assertEquals(1, deleted);
        assertTrue(flashCardRepository.findById(orphan.getId()).isEmpty());
        assertTrue(flashCardRepository.findById(used.getId()).isPresent());
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
        return persistCard(null, front, back);
    }

    private FlashCard persistCard(User owner, String front, String back) {
        return entityManager.persistAndFlush(FlashCard.builder()
                .createdBy(owner)
                .frontContent(front)
                .backContent(back)
                .build());
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
