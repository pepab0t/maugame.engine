package dev.cerios.maugame.mauengine.game.action;

public record RegisterAction(String playerId, boolean isMe) implements Action {
    @Override
    public ActionType type() {
        return ActionType.REGISTER_PLAYER;
    }
}
