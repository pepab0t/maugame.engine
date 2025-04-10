package dev.cerios.mauengine.game.action;

public record PlayerShiftAction(String nextPlayerId) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.PLAYER_CHANGE;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
