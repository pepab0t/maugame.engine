package dev.cerios.mauengine.game.action;

public record RegisterAction(String playerId) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.REGISTER_PLAYER;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
