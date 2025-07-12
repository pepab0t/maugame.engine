package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record HiddenDrawAction(
        Player player,
        int count
) implements Action {

    @Override
    public ActionType getType() {
        return ActionType.HIDDEN_DRAW;
    }
}
