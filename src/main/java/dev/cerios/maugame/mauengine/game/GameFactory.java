package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardComparer;
import dev.cerios.maugame.mauengine.card.CardManager;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class GameFactory {
    private final Random random;

    public Game createGame(Random random, int minPlayers, int maxPlayers) {
        PlayerManager playerManager = new PlayerManager(random, minPlayers, maxPlayers);
        var cardManager = CardManager.create(random, new CardComparer());
        GameCore core = new GameCore(cardManager, playerManager);
        return new Game(core, playerManager);
    }

    public Game createGame(int minPlayers, int maxPlayers) {
        return createGame(random, minPlayers, maxPlayers);
    }

    public Game createGame() {
        return this.createGame(random, 2, 2);
    }
}
