package dev.cerios.maugame.mauengine.card;

import dev.cerios.maugame.mauengine.exception.CardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardManagerTest {

    private CardManager cardManager;
    @Mock
    private CardComparer comparer;

    @BeforeEach
    void setUp() {
        var random = new Random(868);
        cardManager = new CardManager(
                List.of(
                        new Card(CardType.QUEEN, Color.HEARTS),
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                ),
                random,
                comparer
        );
    }

    @Test
    void whenPileNotStarted_thenDeckContainsAll() {
        // when
        int deckSize = cardManager.deckSize();
        Card card = cardManager.peekPile();

        // then
        assertThat(deckSize).isEqualTo(5);
        assertThat(card).isNull();
    }

    @Test
    void whenPileStarted_pileCardRemovedFromDeck() {
        // when
        Card pileCard = cardManager.startPile();
        int deckSize = cardManager.deckSize();
        Card peekedCard = cardManager.peekPile();

        // then
        assertThat(deckSize).isEqualTo(4);
        assertThat(peekedCard).isSameAs(pileCard);
        assertThat(pileCard).isEqualTo(new Card(CardType.QUEEN, Color.HEARTS));
    }

    @Test
    void whenDrawOneCard_thenCardRemovedFromDeck() throws Exception {
        // when
        Card drawnCard = cardManager.draw();

        // then
        assertThat(cardManager.deckSize()).isEqualTo(4);
        assertThat(drawnCard).isEqualTo(new Card(CardType.QUEEN, Color.HEARTS));
        assertThat(
                ((Queue<Card>) getField(cardManager, "deck"))
                        .stream()
                        .toList()
        )
                .containsExactly(
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                );
    }

    @Test
    void whenDrawNCards_thenCardsRemovedFromDeck() throws Exception {
        // when
        var drawnCards = cardManager.draw(4);

        // then
        assertThat(cardManager.deckSize()).isEqualTo(1);
        assertThat(drawnCards).containsExactly(
                new Card(CardType.QUEEN, Color.HEARTS),
                new Card(CardType.SEVEN, Color.SPADES),
                new Card(CardType.EIGHT, Color.DIAMONDS),
                new Card(CardType.NINE, Color.HEARTS)
        );
        assertThat(
                ((Queue<Card>) getField(cardManager, "deck"))
                        .stream()
                        .toList()
        )
                .containsExactly(
                        new Card(CardType.KING, Color.CLUBS)
                );
    }

    @Test
    void whenOneCardDrawnTooManyTimes_thenThrow() throws CardException {
        // setup
        for (int i = 0; i < 4; i++) {
            cardManager.draw();
        }

        // when, then
        assertThatThrownBy(() -> cardManager.draw())
                .isInstanceOf(CardException.class);
    }

    @Test
    void whenDrawMoreThanDeckContains_thenThrow() {
        // when, then
        assertThatThrownBy(() -> cardManager.draw(5))
                .isInstanceOf(CardException.class);
    }

    @Test
    void whenCardManagerInitializedWithEmptyCollection_thenThrow() {
        // when, then
        assertThatThrownBy(() -> new CardManager(Collections.emptyList(), new Random(), comparer))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void whenPlayValidCard_pileAndDeckShouldUpdate() throws Exception {
        // setup
        Card pileCard = cardManager.startPile();
        Card cardToPlay = cardManager.draw();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(true);

        // when
        boolean canBePlayed = cardManager.playCard(cardToPlay, null);

        // then
        assertThat(canBePlayed).isTrue();
        assertThat(((Queue<Card>) getField(cardManager, "deck")).stream())
                .containsExactly(
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS),
                        new Card(CardType.QUEEN, Color.HEARTS)
                );
        assertThat(cardManager.deckSize()).isEqualTo(4);
        assertThat(cardManager.peekPile()).isEqualTo(cardToPlay);
        verify(comparer).clear();
    }

    @Test
    void whenPlayInvalidCard_thenDontUpdate() throws Exception {
        // setup
        Card pileCard = cardManager.startPile();
        Card cardToPlay = cardManager.draw();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(false);

        // when
        boolean canBePlayed = cardManager.playCard(cardToPlay, null);

        // then
        assertThat(canBePlayed).isFalse();
        assertThat(((Queue<Card>) getField(cardManager, "deck")).stream())
                .containsExactly(
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                );
        assertThat(cardManager.deckSize()).isEqualTo(3);
        assertThat(cardManager.peekPile()).isEqualTo(pileCard);
    }

    @Test
    void whenPlayNonQueenAndColorProvided_thenIgnoreIt() throws CardException {
        // setup
        Card pileCard = cardManager.startPile();
        Card cardToPlay = cardManager.draw();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(true);

        // when
        boolean canBePlayed = cardManager.playCard(cardToPlay, Color.SPADES);

        // then
        assertThat(canBePlayed).isTrue();
        verify(comparer, never()).setNextColor(any(Color.class));
        assertThat(cardManager.deckSize()).isEqualTo(4);
        assertThat(cardManager.peekPile()).isEqualTo(cardToPlay);
    }

    @Test
    void whenPlayQueenAndColorNotProvided_thenThrow() throws CardException {
        // setup
        Card cardToPlay = cardManager.draw();
        Card pileCard = cardManager.startPile();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> cardManager.playCard(cardToPlay, null))
                .isInstanceOf(CardException.class);
    }

    @Test
    void whenPlayQueenAndColorProvided_registerIt() throws CardException {
        // setup
        Card cardToPlay = cardManager.draw();
        Card pileCard = cardManager.startPile();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(true);

        // when
        boolean canBePlayed = cardManager.playCard(cardToPlay, Color.SPADES);

        // then
        assertThat(canBePlayed).isTrue();
        verify(comparer).setNextColor(any(Color.class));
        verify(comparer, never()).clear();
    }

    @Test
    void whenCardPlayedAndPileNotStarted_thenThrow() {
        // when
        assertThatThrownBy(() -> cardManager.playCard(new Card(CardType.JACK, Color.CLUBS), null))
                .isInstanceOf(CardException.class);
    }

    @Test
    void testShuffleRemainingCardsCorrectly() throws Exception {
        // when
        cardManager.shuffleRemaining();

        // then
        cardManager.shuffleRemaining();
        assertThat(((Queue<Card>) getField(cardManager, "deck")).stream())
                .containsExactlyInAnyOrder(
                        new Card(CardType.QUEEN, Color.HEARTS),
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                )
                .doesNotContainSequence(
                        new Card(CardType.QUEEN, Color.HEARTS),
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                );
    }

    @Test
    void testShuffleRemainingCardsWithPileCorrectly() throws Exception {
        // setup
        Card pileCard = cardManager.startPile();

        // when
        cardManager.shuffleRemaining();

        // than
        cardManager.shuffleRemaining();
        assertThat(((Queue<Card>) getField(cardManager, "deck")).stream())
                .containsExactlyInAnyOrder(
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                )
                .doesNotContainSequence(
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS)
                );
        assertThat(cardManager.peekPile()).isEqualTo(pileCard);
    }

    @Test
    void factoryIncludesFullSetOfCards() throws Exception {
        // when
        var created = CardManager.create(new Random(878), comparer);
        var cardSet = new HashSet<>(((Queue<Card>) getField(created, "deck")));

        // then
        Set<Card> expected = new HashSet<>();
        for (Color color : Color.values()) {
            for (CardType cardType : CardType.values()) {
                expected.add(new Card(cardType, color));
            }
        }
        assertThat(cardSet).containsExactlyInAnyOrderElementsOf(expected);
    }

    public static Object getField(Object object, String fieldName) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}