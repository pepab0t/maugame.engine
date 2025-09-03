package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardComparer;
import dev.cerios.maugame.mauengine.card.CardManager;
import lombok.RequiredArgsConstructor;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class GameFactory {
    private final Random random;

    public Game createGame(Random random, int minPlayers, int maxPlayers) {
        var gameId = UUID.randomUUID();
        var stage = new AtomicReference<>(Stage.LOBBY);
        var cardManager = CardManager.create(random, new CardComparer());
        PlayerManager playerManager = new PlayerManager(gameId, random, minPlayers, maxPlayers, stage, cardManager);
        GameCore core = new GameCore(cardManager, playerManager, stage);
        return new Game(gameId, core, playerManager);
    }

    public Game createGame(int minPlayers, int maxPlayers) {
        return createGame(random, minPlayers, maxPlayers);
    }

    public Game createGame() {
        return createGame(random, 2, 2);
    }
}
