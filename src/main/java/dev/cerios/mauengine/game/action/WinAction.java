package dev.cerios.mauengine.game.action;

public record WinAction(String playerId) implements Action, PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.WIN;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return this;
    }
}
