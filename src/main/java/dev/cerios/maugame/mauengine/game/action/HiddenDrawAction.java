package dev.cerios.maugame.mauengine.game.action;

public record HiddenDrawAction(
        String playerId,
        byte count
) implements Action {

    @Override
    public ActionType type() {
        return ActionType.HIDDEN_DRAW;
    }
}
