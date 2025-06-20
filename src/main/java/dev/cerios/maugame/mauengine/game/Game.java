package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
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

    public void playCardMove(final String playerId, Card cardToPlay, Optional<Color> nextColor) throws MauEngineBaseException {
        if (nextColor.isPresent()) {
            core.performPlayCard(playerId, cardToPlay, nextColor.get());
        }
        core.performPlayCard(playerId, cardToPlay);
    }

    public void playDrawMove(final String playerId, int count) throws MauEngineBaseException {
        core.performDraw(playerId, count);
    }

    public void playPassMove(final String playerId) throws MauEngineBaseException {
        core.performPass(playerId);
    }

    public Player registerPlayer(final GameEventListener eventListener) throws GameException {
        if (core.getStage() != LOBBY) {
            throw new GameException("The game has already started.");
        }
        var player = playerManager.registerPlayer(eventListener);
        var action = new PlayersAction(playerManager.getPlayers());
        player.trigger(action);
        return player;
    }

    public int getFreeCapacity() {
        return playerManager.getFreeCapacity();
    }

    public void start() throws MauEngineBaseException {
        playerManager.distributeActionToAll(new StartAction(uuid.toString()));
        core.start();
    }

    public ActivateAction activatePlayer(String playerId) {
        return playerManager.activatePlayer(playerId);
    }

    public DeactivateAction deactivatePlayer(String playerId) {
        return playerManager.deactivatePlayer(playerId);
    }

    public Stage getStage() {
        return core.getStage();
    }

    public List<String> getAllPlayers() {
        return playerManager.getPlayers().stream().map(Player::getPlayerId).toList();
    }
}
