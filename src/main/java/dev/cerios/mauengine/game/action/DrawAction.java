package dev.cerios.mauengine.game.action;

import dev.cerios.mauengine.card.Card;

import java.util.List;

public record DrawAction(String playerId, List<Card> cardsDrawn) implements Action {
    @Override
    public ActionType type() {
        return ActionType.DRAW;
    }
}
