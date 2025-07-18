package dev.cerios.maugame.mauengine.card;

import lombok.Getter;

@Getter
public enum CardType {
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    JACK("J"),
    QUEEN("Q"),
    KING("K"),
    ACE("A");

    private final String symbol;

    CardType(String symbol) {
        this.symbol = symbol;
    }
}
