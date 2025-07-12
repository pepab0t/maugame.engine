package dev.cerios.maugame.mauengine.game.action;

public record ActivateAction(String playerId) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.ACTIVATE_PLAYER;
    }
}
