package dev.cerios.maugame.mauengine.game.action;

import java.util.List;

public record EndAction(List<String> playerRank) implements Action {
    @Override
    public ActionType getType() {
        return ActionType.END_GAME;
    }
}
