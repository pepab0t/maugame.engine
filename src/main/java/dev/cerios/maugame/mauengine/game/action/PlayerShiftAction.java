package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record PlayerShiftAction(Player player) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.PLAYER_SHIFT;
    }
}
