package dev.cerios.maugame.mauengine.card;

import lombok.Getter;

@Getter
public enum Color {
    DIAMONDS("♦"),
    CLUBS("♣"),
    HEARTS("♥"),
    SPADES("♠");

    private final String symbol;

    Color(String symbol) {
        this.symbol = symbol;
    }
}
