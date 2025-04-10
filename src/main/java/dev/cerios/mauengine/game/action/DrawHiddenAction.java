package dev.cerios.mauengine.game.action;

import dev.cerios.mauengine.game.action.Action.ActionType;

public record DrawHiddenAction(String playerId, int count) implements PrivateAction {
    @Override
    public ActionType type() {
        return ActionType.DRAW;
    }
}
