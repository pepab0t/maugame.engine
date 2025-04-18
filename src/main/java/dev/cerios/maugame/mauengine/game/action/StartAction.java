package dev.cerios.maugame.mauengine.game.action;

public record StartAction() implements Action {
    @Override
    public ActionType type() {
        return ActionType.START_GAME;
    }
}
