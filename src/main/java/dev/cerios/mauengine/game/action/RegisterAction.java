package dev.cerios.mauengine.game.action;

public record RegisterAction(String playerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.REGISTER_PLAYER;
    }
}
