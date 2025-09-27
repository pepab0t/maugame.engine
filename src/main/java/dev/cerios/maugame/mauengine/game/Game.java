package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.NotSupportedOperation;
import dev.cerios.maugame.mauengine.player.PlayerContext;
import dev.cerios.maugame.mauengine.player.PlayerLobbyState;
import dev.cerios.maugame.mauengine.player.PlayerReadyStorage;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class Game {
    private final UUID gameId;
    private final GameCore core;
    private final PlayerContext playerContext;
    private final ReadWriteLock lock;

    private final List<Consumer<UUID>> startListeners = Collections.synchronizedList(new LinkedList<>());

    Game(UUID gameId, GameCore core, PlayerContext playerContext, ReadWriteLock lock) {
        this.gameId = gameId;
        this.core = core;
        this.playerContext = playerContext;
        this.lock = lock;

        this.playerContext.listenStartGame(uuid -> startListeners.forEach(l -> l.accept(uuid)));
    }

    public void listenStart(Consumer<UUID> startListener) {
        this.startListeners.add(startListener);
    }

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

    public GamePlayer registerPlayer(String username, final GameEventListener eventListener) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            return playerContext.getPlayers().registerPlayer(username, eventListener);
        } finally {
            l.unlock();
        }
    }

    public void removePlayer(String playerId) {
        var l = lock.writeLock();
        try {
            l.lock();
            playerContext.getPlayers().removePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public GamePlayer getPlayer(String playerId) throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            return playerContext.getPlayers().getPlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public Collection<? extends GamePlayer> getAllPlayers() {
        var l = lock.readLock();
        try {
            l.lock();
            return playerContext.getPlayers().getPlayers();
        } finally {
            l.unlock();
        }
    }

    public void setReady(String playerId) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            if (playerContext.getPlayers() instanceof PlayerReadyStorage players) {
                players.setReady(playerId);
            } else {
                throw new NotSupportedOperation("set ready", playerContext.getPlayers().getClass());
            }
        } finally {
            l.unlock();
        }
    }

    public int getFreeCapacity() {
        var l = lock.readLock();
        try {
            l.lock();
            return playerContext.getPlayers() instanceof PlayerLobbyState players ? players.getFreeCapacity() : 0;
        } finally {
            l.unlock();
        }
    }

    public int getPlayerCount() {
        var l = lock.readLock();
        try {
            l.lock();
            return playerContext.getPlayers().getPlayers().size();
        } finally {
            l.unlock();
        }
    }

    public UUID getId() {
        return gameId;
    }

    public void sendCurrentStateTo(String playerId, Predicate<GamePlayer> playerMatcher) throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            core.sendCurrentStateTo(playerId, playerMatcher);
        } finally {
            l.unlock();
        }
    }
}
