package dev.cerios.maugame.mauengine.game.action;

public interface Action {
    ActionType type();

    enum ActionType {
        REGISTER_PLAYER,
        ACTIVATE_PLAYER,
        DEACTIVATE_PLAYER,
        PLAY_CARD,
        DRAW,
        PASS,
        PLAYER_CHANGE,
        START_GAME,
        END_GAME,
        WIN,
        LOSE,
        PLAYER_RANK,
        START_PILE;
    }
}
