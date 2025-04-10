package dev.cerios.mauengine.game.action;

import java.util.List;

public record PlayerRankAction(List<String> playerRank) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAYER_RANK;
    }
}
