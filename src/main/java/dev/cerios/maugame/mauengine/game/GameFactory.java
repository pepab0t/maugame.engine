package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardComparer;
import dev.cerios.maugame.mauengine.card.CardManager;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class GameFactory {
    private final Random random;

    public Game createGame(Random random) {
        PlayerManager playerManager = new PlayerManager(random);
        GameCore core = new GameCore(CardManager.create(random), new CardComparer(), playerManager);
        return new Game(core, playerManager);
    }

    public Game createGame() {
        return this.createGame(random);
    }
}
