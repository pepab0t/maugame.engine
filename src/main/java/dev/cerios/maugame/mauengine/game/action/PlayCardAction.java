package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.game.Player;

public record PlayCardAction(Player player, Card card, Color nextColor) implements Action {

    public PlayCardAction(Player player, Card card) {
        this(player, card, null);
    }

    @Override
    public ActionType getType() {
        return ActionType.PLAY_CARD;
    }
}