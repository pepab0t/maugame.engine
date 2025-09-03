package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

import java.util.UUID;

public record RegisterAction(UUID gameId, Player player, boolean isMe) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.REGISTER_PLAYER;
    }
}
