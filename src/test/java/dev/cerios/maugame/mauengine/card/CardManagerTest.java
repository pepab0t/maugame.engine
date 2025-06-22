package dev.cerios.maugame.mauengine.card;

import dev.cerios.maugame.mauengine.exception.CardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardManagerTest {

    private CardManager cardManager;

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
                random
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
    void whenDrawMoreThanDeckContains_thenThrow() {
        // when, then
        assertThatThrownBy(() -> cardManager.draw(5))
                .isInstanceOf(CardException.class);
    }

    public static Object getField(Object object, String fieldName) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}