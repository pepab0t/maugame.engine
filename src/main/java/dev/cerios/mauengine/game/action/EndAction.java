package dev.cerios.mauengine.game.action;

public record EndAction() implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.END_GAME;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
