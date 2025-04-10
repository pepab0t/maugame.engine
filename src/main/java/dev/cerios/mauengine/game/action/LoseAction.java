package dev.cerios.mauengine.game.action;

public record LoseAction(String playerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.LOSE;
    }
}
