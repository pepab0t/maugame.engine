package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.player.Player;

public record DeactivateAction(Player player) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.DEACTIVATE_PLAYER;
    }
}
