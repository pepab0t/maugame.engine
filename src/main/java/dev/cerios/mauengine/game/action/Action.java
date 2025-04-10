package dev.cerios.mauengine.game.action;

public interface Action {
    ActionType type();

    PrivateAction hide(String playerId);

    enum ActionType {
        REGISTER_PLAYER,
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
