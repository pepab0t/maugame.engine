package dev.cerios.maugame.mauengine.card;

public record Card(CardType type, Color color) {
    public String symbol() {
        return color.getSymbol() + type.getSymbol();
    }
}
