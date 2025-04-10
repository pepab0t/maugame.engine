package dev.cerios.mauengine.game.action;

import dev.cerios.mauengine.card.Card;

public record StartPileAction(Card card) implements Action {

    @Override
    public ActionType type() {
        return ActionType.START_PILE;
    }
}
