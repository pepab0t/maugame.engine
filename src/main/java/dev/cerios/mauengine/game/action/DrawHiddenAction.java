package dev.cerios.mauengine.game.action;

public record DrawHiddenAction(String playerId, int count) implements Action {
    @Override
    public ActionType type() {
        return null;
    }
}
