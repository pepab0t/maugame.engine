package dev.cerios.maugame.mauengine.card;

public record Card(CardType type, Color color) {
    @Override
    public String toString() {
        return type.getSymbol() + color.getSymbol();
    }
}
