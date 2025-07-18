package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;

import java.util.List;

public record DrawAction(List<Card> cardsDrawn) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.DRAW;
    }
}
