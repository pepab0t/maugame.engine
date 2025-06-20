package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record HiddenDrawAction(
        Player player,
        byte count
) implements Action {

    @Override
    public ActionType type() {
        return ActionType.HIDDEN_DRAW;
    }
}
