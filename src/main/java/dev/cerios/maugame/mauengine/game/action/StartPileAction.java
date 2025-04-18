package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;

public record StartPileAction(Card card) implements Action {

    @Override
    public ActionType type() {
        return ActionType.START_PILE;
    }
}
