package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.move.DrawMove;
import dev.cerios.maugame.mauengine.game.move.PassMove;
import dev.cerios.maugame.mauengine.game.move.PlayCardMove;
import dev.cerios.maugame.mauengine.player.PlayerManager;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;


@RequiredArgsConstructor
public class Game {
    private final GameCore core;
    private final PlayerManager playerManager;

    public PlayCardMove createPlayMove(final String playerId, Card cardToPlay, Optional<Color> nextColor) {
        return nextColor
                .map(color -> new PlayCardMove(core, playerId, cardToPlay, color))
                .orElseGet(() -> new PlayCardMove(core, playerId, cardToPlay));
    }

    public DrawMove createDrawMove(final String playerId, int count) {
        return new DrawMove(core, playerId, count);
    }

    public PassMove createPassMove(final String playerId) {
        return new PassMove(core, playerId);
    }

    public void registerPlayer(final String playerId) throws GameException {
        if (core.getStage() != LOBBY) {
            throw new GameException("The game has already started.");
        }
        playerManager.registerPlayer(playerId);
    }

    public int getFreeCapacity() {
        return playerManager.getFreeCapacity();
    }

    public void start() throws MauEngineBaseException {
        core.start();
    }
}
