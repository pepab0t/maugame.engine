package dev.cerios.maugame.mauengine.game.action;

import java.util.List;

public record SendRankAction(List<String> playerRank) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAYER_RANK;
    }
}
