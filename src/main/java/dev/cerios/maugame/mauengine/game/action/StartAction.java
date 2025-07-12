package dev.cerios.maugame.mauengine.game.action;

public record StartAction(String gameId) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.START_GAME;
    }
}
