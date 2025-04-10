package dev.cerios.mauengine.game.move;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.card.Color;

public class PlayCardMove implements PlayerMove {
    private final String playerId;
    private final Card card;
    private final Color nextColor;

    public PlayCardMove(String playerId, Card card) {
        this(playerId, card, null);
    }

    public PlayCardMove(String playerId, Card card, Color nextColor) {
        this.playerId = playerId;
        this.card = card;
        this.nextColor = nextColor;
    }

    public Color getNextColor() {
        return nextColor;
    }

    @Override
    public String playerId() {
        return playerId;
    }

    public Card getCard() {
        return card;
    }
}
