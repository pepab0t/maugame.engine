package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;

public record StartPileAction(Card card) implements Action {

    @Override
    public ActionType getType() {
        return ActionType.START_PILE;
    }
}
