package dev.cerios.mauengine.game.action;

public record PassAction(String playerId) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.PASS;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
