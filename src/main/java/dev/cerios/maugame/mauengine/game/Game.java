package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.StartAction;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;


@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {
    @Getter
    @EqualsAndHashCode.Include
    private final UUID uuid = UUID.randomUUID();
    private final GameCore core;
    private final PlayerManager playerManager;
    private final CardManager cardManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void playCardMove(final String playerId, Card cardToPlay) throws MauEngineBaseException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.performPlayCard(playerId, cardToPlay);
        } finally {
            l.unlock();
        }
    }

    public void playCardMove(final String playerId, Card cardToPlay, Color nextColor) throws MauEngineBaseException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.performPlayCard(playerId, cardToPlay, nextColor);
        } finally {
            l.unlock();
        }
    }

    public void playDrawMove(final String playerId) throws MauEngineBaseException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.performDraw(playerId);
        } finally {
            l.unlock();
        }
    }

    public void playPassMove(final String playerId) throws MauEngineBaseException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.performPass(playerId);
        } finally {
            l.unlock();
        }
    }

    public Player registerPlayer(String username, final GameEventListener eventListener) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            if (core.getStage() != LOBBY) {
                throw new GameException("The game has already started.");
            }
            return playerManager.registerPlayer(username, eventListener);
        } finally {
            l.unlock();
        }
    }

    public void removePlayer(String playerId) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            playerManager.removePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public GameState getGameState() {
        var l = lock.readLock();
        try {
            l.lock();
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
        } finally {
            l.unlock();
        }
    }

    public int getFreeCapacity() {
        var l = lock.readLock();
        try {
            l.lock();
            return playerManager.getFreeCapacity();
        } finally {
            l.unlock();
        }
    }

    public void start() throws MauEngineBaseException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.start();
            playerManager.distributeActionToAll(new StartAction(uuid.toString()));
        } finally {
            l.unlock();
        }
    }

    public void activatePlayer(String playerId) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            playerManager.activatePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public void deactivatePlayer(String playerId) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            if (core.getStage() == LOBBY) {
                playerManager.removePlayer(playerId);
                return;
            }
            playerManager.deactivatePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public Stage getStage() {
        var l = lock.readLock();
        try {
            l.lock();
            return core.getStage();
        } finally {
            l.unlock();
        }
    }

    public List<Player> getAllPlayers() {
        var l = lock.readLock();
        try {
            l.lock();
            return playerManager.getPlayers();
        } finally {
            l.unlock();
        }
    }
}
