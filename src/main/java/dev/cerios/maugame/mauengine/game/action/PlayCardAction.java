package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;

public record PlayCardAction(String playerId, Card card) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAY_CARD;
    }
}