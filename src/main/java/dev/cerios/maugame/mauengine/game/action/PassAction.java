package dev.cerios.maugame.mauengine.game.action;

public record PassAction(String playerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PASS;
    }
}
