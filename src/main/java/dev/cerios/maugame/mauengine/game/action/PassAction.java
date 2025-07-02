package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

public record PassAction(Player player) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PASS;
    }
}
