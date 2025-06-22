package dev.cerios.maugame.mauengine.card;

import dev.cerios.maugame.mauengine.exception.CardException;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CardManager {
    private final Queue<Card> deck;
    private final Queue<Card> pile;
    private final Random random;

    private final CardComparer cardComparer;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static CardManager create(Random random, CardComparer cardComparer) {
        Set<Card> cards = new HashSet<>();
        for (CardType type : CardType.values()) {
            for (Color color : Color.values()) {
                cards.add(new Card(type, color));
            }
        }
        return new CardManager(cards, random, cardComparer)
                .shuffleRemaining();
    }

    public CardManager(Collection<Card> cards, Random random, CardComparer cardComparer) {
        if (cards.isEmpty())
            throw new IllegalStateException("Cards must not be empty");
        this.deck = new LinkedList<>(cards);
        this.pile = new LinkedList<>();
        this.random = random;
        this.cardComparer = cardComparer;
    }

    public CardManager shuffleRemaining() {
        try {
            lock.writeLock().lock();
            List<Card> cardList = new ArrayList<>(Color.values().length * CardType.values().length);
            while (!deck.isEmpty()) {
                cardList.add(deck.remove());
            }
            Collections.shuffle(cardList, random);
            deck.addAll(cardList);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Card draw() throws CardException {
        try {
            lock.writeLock().lock();
            if (deck.size() + pile.size() < 2)
                throw new CardException("Cannot draw more cards");
            return deck.remove();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Card> draw(int n) throws CardException {
        try {
            lock.writeLock().lock();
            if (deck.size() + pile.size() < n + 1) {
                throw new CardException("Cannot draw " + n + " cards, only " + (deck.size() - 1) + " cards are available.");
            }
            List<Card> cardList = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                cardList.add(deck.remove());
            }
            return cardList;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Card startPile() {
        try {
            lock.writeLock().lock();
            Card card = deck.remove();
            pile.add(card);
            return card;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Card peekPile() {
        try {
            lock.readLock().lock();
            return pile.peek();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean playCard(Card card, Color nextColor) throws CardException {
        try {
            lock.writeLock().lock();
            var pileCard = pile.peek();
            if (pileCard == null)
                throw new CardException("Pile not started");

            if (!cardComparer.compare(pileCard, card))
                return false;

            if (card.type() == CardType.QUEEN) {
                if (nextColor == null)
                    throw new CardException("Next color not specified, when played QUEEN.");
                cardComparer.setNextColor(nextColor);
            } else {
                cardComparer.clear();
            }
            pile.add(card);
            deck.add(pile.remove());
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int deckSize() {
        try {
            lock.readLock().lock();
            return deck.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
