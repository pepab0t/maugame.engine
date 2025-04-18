package dev.cerios.maugame.mauengine.game.action;

public record PlayerShiftAction(String nextPlayerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAYER_CHANGE;
    }
}
