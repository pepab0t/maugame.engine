package dev.cerios.mauengine.game.action;

public record LoseAction(String playerId) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.LOSE;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
