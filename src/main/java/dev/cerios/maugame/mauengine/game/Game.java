package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.NotSupportedOperation;
import dev.cerios.maugame.mauengine.player.PlayerContext;
import dev.cerios.maugame.mauengine.player.PlayerReadyStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;


@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Game {

    private final GameCore core;
    private final PlayerContext playerContext;
    private final ReadWriteLock lock;

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
