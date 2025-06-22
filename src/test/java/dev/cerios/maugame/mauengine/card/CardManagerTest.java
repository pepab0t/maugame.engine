package dev.cerios.maugame.mauengine.card;

import dev.cerios.maugame.mauengine.exception.CardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
                        new Card(CardType.JACK, Color.HEARTS),
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
        assertThat(pileCard).isEqualTo(new Card(CardType.JACK, Color.HEARTS));
    }

    @Test
    void whenDrawOneCard_thenCardRemovedFromDeck() throws Exception {
        // when
        Card drawnCard = cardManager.draw();

        // then
        assertThat(cardManager.deckSize()).isEqualTo(4);
        assertThat(drawnCard).isEqualTo(new Card(CardType.JACK, Color.HEARTS));
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
                new Card(CardType.JACK, Color.HEARTS),
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
    void whenPlayCard_pileAndDeckShouldUpdate() throws Exception {
        // setup
        Card cardToPlay = new Card(CardType.ACE, Color.HEARTS);
        Card pileCard = cardManager.startPile();

        when(comparer.compare(pileCard, cardToPlay)).thenReturn(true);

        // when
        boolean canBePlayed = cardManager.playCard(cardToPlay, null);

        // then
        assertThat(canBePlayed).isTrue();
        assertThat(((Queue<Card>) getField(cardManager, "deck")).stream())
                .containsExactly(
                        new Card(CardType.SEVEN, Color.SPADES),
                        new Card(CardType.EIGHT, Color.DIAMONDS),
                        new Card(CardType.NINE, Color.HEARTS),
                        new Card(CardType.KING, Color.CLUBS),
                        new Card(CardType.JACK, Color.HEARTS)
                );
        assertThat(cardManager.deckSize()).isEqualTo(5);
        assertThat(cardManager.peekPile()).isEqualTo(cardToPlay);
    }

    @Test
    void whenCardPlayedAndPileNotStarted_thenThrow() {

    }

    public static Object getField(Object object, String fieldName) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}