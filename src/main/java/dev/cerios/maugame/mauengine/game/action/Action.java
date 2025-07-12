package dev.cerios.maugame.mauengine.game.action;

public interface Action {
    ActionType getType();

    enum ActionType {
        REGISTER_PLAYER,
        ACTIVATE_PLAYER,
        DEACTIVATE_PLAYER,
        PLAY_CARD,
        DRAW,
        HIDDEN_DRAW,
        PASS,
        PLAYER_SHIFT,
        START_GAME,
        END_GAME,
        WIN,
        LOSE,
        PLAYER_RANK,
        PLAYERS,
        START_PILE,
        REMOVE_PLAYER;
    }
}
