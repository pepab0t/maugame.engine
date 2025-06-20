package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record RegisterAction(Player player, boolean isMe) implements Action {
    @Override
    public ActionType type() {
        return ActionType.REGISTER_PLAYER;
    }
}
