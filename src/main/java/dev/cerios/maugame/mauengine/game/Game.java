package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.StartAction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
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
    private final CardManager cardManager;

    public void playCardMove(final String playerId, Card cardToPlay) throws MauEngineBaseException {
        core.performPlayCard(playerId, cardToPlay);
    }

    public void playCardMove(final String playerId, Card cardToPlay, Color nextColor) throws MauEngineBaseException {
        core.performPlayCard(playerId, cardToPlay, nextColor);
    }

    public void playDrawMove(final String playerId) throws MauEngineBaseException {
        core.performDraw(playerId);
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
        return new GameState(
                playerManager.getPlayerRank().stream().toList(),
                playerManager.getPlayers().stream().collect(
                        HashMap::new,
                        (map, player) -> map.put(player.getUsername(), player.getHand()),
                        HashMap::putAll
                ),
                cardManager.peekPile(),
                cardManager.deckSize(),
                core.getStage(),
                playerManager.currentPlayer().getUsername(),
                core.getGameEffect()
        );
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
