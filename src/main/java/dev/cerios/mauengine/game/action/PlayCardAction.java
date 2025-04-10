package dev.cerios.mauengine.game.action;

import dev.cerios.mauengine.card.Card;

public record PlayCardAction(String playerId, Card card) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.PLAY_CARD;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}