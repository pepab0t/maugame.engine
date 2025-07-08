package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record RemovePlayerAction(Player player) implements Action {
    @Override
    public ActionType type() {
        return ActionType.REMOVE_PLAYER;
    }
}
