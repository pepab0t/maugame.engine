package dev.cerios.maugame.mauengine.game.action;

public record DeactivateAction(String playerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.DEACTIVATE_PLAYER;
    }
}
