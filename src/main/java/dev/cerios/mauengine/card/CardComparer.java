package dev.cerios.mauengine.card;

public class CardComparer {
    private volatile Color nextColor;

    public boolean compare(Card pileCard, Card newCard) {
        if (nextColor != null)
            return newCard.color() == nextColor;
        return pileCard.color() == newCard.color() || pileCard.type() == newCard.type();
    }

    public void setNextColor(Color nextColor) {
        this.nextColor = nextColor;
    }

    public void clear() {
        nextColor = null;
    }
}
