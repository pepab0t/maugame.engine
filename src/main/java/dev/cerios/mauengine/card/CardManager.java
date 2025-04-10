package dev.cerios.mauengine.card;

import java.util.*;

public class CardManager {
    private final Queue<Card> deck;
    private final Queue<Card> pile;

    public static CardManager create() {
        Set<Card> cards = new HashSet<>();
        for (CardType type : CardType.values()) {
            for (Color color : Color.values()) {
                cards.add(new Card(type, color));
            }
        }
        return new CardManager(cards)
                .shuffleRemaining();
    }

    public CardManager(Collection<Card> cards) {
        this.deck = new LinkedList<>(cards);
        this.pile = new LinkedList<>();
    }

    public CardManager shuffleRemaining() {
        List<Card> cardList = new ArrayList<>(Color.values().length * CardType.values().length);
        while (!deck.isEmpty()) {
            cardList.add(deck.remove());
        }
        Collections.shuffle(cardList);
        deck.addAll(cardList);
        return this;
    }

    public Card draw() throws NoSuchElementException {
        return deck.remove();
    }

    public List<Card> draw(int n) {
        if (deck.size() < n) {
            throw new NoSuchElementException("Cannot draw " + n + " cards, only " + deck.size() + " cards are available.");
        }
        List<Card> cardList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            cardList.add(deck.remove());
        }
        return cardList;
    }

    public void add(List<Card> cards) {
        this.deck.addAll(cards);
    }

    public Card startPile() {
        Card card = deck.remove();
        pile.add(card);
        return card;
    }

    public Card peekPile() {
        return pile.peek();
    }

    public void putCard(Card card) {
        pile.add(card);
        deck.add(pile.remove());
    }

    public int deckSize() {
        return deck.size();
    }
}
