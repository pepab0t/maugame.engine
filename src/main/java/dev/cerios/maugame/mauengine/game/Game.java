package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.NotSupportedOperation;
import dev.cerios.maugame.mauengine.player.Player;
import dev.cerios.maugame.mauengine.player.PlayerContext;
import dev.cerios.maugame.mauengine.player.PlayerReadyStorage;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.SequencedCollection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;


@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {

    @EqualsAndHashCode.Include
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

    public Player registerPlayer(String username, final GameEventListener eventListener) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            return core.registerPlayer(username, eventListener);
        } finally {
            l.unlock();
        }
    }

    public void removePlayer(String playerId) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            core.removePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public Player getPlayer(String playerId) throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            return core.getPlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public Collection<Player> getAllPlayers() {
        var l = lock.readLock();
        try {
            l.lock();
            return core.getPlayers();
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

    public void sendCurrentStateTo(String playerId, Predicate<Player> playerMatcher) throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            core.sendCurrentStateTo(playerId, playerMatcher);
        } finally {
            l.unlock();
        }
    }
}
