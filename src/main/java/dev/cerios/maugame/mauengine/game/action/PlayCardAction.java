package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.Player;

public record PlayCardAction(Player player, Card card) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAY_CARD;
    }
}