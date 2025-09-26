package dev.cerios.maugame.mauengine.game.action;

import java.util.UUID;

public record DestroyAction(UUID gameId) implements Action {

    @Override
    public ActionType getType() {
        return ActionType.DESTROY;
    }
}
