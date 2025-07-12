package dev.cerios.maugame.mauengine.game.action;

public record EndAction() implements Action {
    @Override
    public ActionType getType() {
        return ActionType.END_GAME;
    }
}
