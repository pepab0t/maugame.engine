package dev.cerios.maugame.mauengine.game.action;

public record UnreadyAction(String username) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.UNREADY;
    }
}
