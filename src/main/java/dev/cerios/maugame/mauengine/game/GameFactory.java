package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardComparer;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.player.PlayerContext;
import dev.cerios.maugame.mauengine.player.PlayerRunningState;
import dev.cerios.maugame.mauengine.player.PlayerStateFactory;
import lombok.RequiredArgsConstructor;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RequiredArgsConstructor
public class GameFactory {
    private final Random random;

    public Game createGame(Random random, int minPlayers, int maxPlayers, long turnTimeoutMs) {
        var gameId = UUID.randomUUID();
        var globalLock = new ReentrantReadWriteLock(true);
        var factory = new PlayerStateFactory(gameId, minPlayers, maxPlayers, random, globalLock, turnTimeoutMs);
        var playerContext = new PlayerContext(factory);
        var core = new GameCore(CardManager.create(random, new CardComparer()), playerContext);
        var game = new Game(gameId, core, playerContext, globalLock);
        playerContext.setLobbyState();
        return game;
    }

    public Game createGame(int minPlayers, int maxPlayers, long turnTimeoutMs) {
        return createGame(random, minPlayers, maxPlayers, turnTimeoutMs);
    }

    public Game createGame() {
        return createGame(2, 5, 60_000);
    }
}
