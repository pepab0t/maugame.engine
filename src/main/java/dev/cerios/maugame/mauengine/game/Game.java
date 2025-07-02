package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.ActivateAction;
import dev.cerios.maugame.mauengine.game.action.DeactivateAction;
import dev.cerios.maugame.mauengine.game.action.PlayersAction;
import dev.cerios.maugame.mauengine.game.action.StartAction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;


@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {
    @Getter
    @EqualsAndHashCode.Include
    private final UUID uuid = UUID.randomUUID();
    private final GameCore core;
    private final PlayerManager playerManager;

    public void playCardMove(final String playerId, Card cardToPlay ) throws MauEngineBaseException {
        core.performPlayCard(playerId, cardToPlay);
    }

    public void playCardMove(final String playerId, Card cardToPlay, Color nextColor) throws MauEngineBaseException {
        core.performPlayCard(playerId, cardToPlay, nextColor);
    }

    public void playDrawMove(final String playerId, int count) throws MauEngineBaseException {
        core.performDraw(playerId, count);
    }

    public void playPassMove(final String playerId) throws MauEngineBaseException {
        core.performPass(playerId);
    }

    public Player registerPlayer(String username, final GameEventListener eventListener) throws GameException {
        if (core.getStage() != LOBBY) {
            throw new GameException("The game has already started.");
        }
        return playerManager.registerPlayer(username, eventListener);
    }

    public GameState getGameState() {
        return core.getCurrentState();
    }

    public int getFreeCapacity() {
        return playerManager.getFreeCapacity();
    }

    public void start() throws MauEngineBaseException {
        playerManager.distributeActionToAll(new StartAction(uuid.toString()));
        core.start();
    }

    public void activatePlayer(String playerId) throws GameException {
        playerManager.activatePlayer(playerId);
    }

    public void deactivatePlayer(String playerId) throws GameException {
        playerManager.deactivatePlayer(playerId);
    }

    public Stage getStage() {
        return core.getStage();
    }

    public List<String> getAllPlayers() {
        return playerManager.getPlayers().stream().map(Player::getPlayerId).toList();
    }
}
