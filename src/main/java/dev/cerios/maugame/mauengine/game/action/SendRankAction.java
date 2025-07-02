package dev.cerios.maugame.mauengine.game.action;

import java.util.SequencedCollection;

public record SendRankAction(SequencedCollection<String> playerRank) implements Action {
    @Override
    public ActionType type() {
        return ActionType.PLAYER_RANK;
    }
}
