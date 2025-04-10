package dev.cerios.mauengine.game.action;

public record StartAction() implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.START_GAME;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
