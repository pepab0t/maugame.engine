package dev.cerios.maugame.mauengine.game.action;

public record WinAction(String playerId) implements Action {
    @Override
    public ActionType type() {
        return ActionType.WIN;
    }
}
