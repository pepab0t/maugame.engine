package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record LoseAction(Player player) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.LOSE;
    }
}
