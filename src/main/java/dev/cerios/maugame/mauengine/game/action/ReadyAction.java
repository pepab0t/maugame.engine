package dev.cerios.maugame.mauengine.game.action;

public record ReadyAction(String username) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.READY;
    }
}
