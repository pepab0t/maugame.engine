package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardComparer;
import dev.cerios.maugame.mauengine.card.CardManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameFactory {

    public Game createGame() {
        PlayerManager playerManager = new PlayerManager();
        GameCore core = new GameCore(CardManager.create(), new CardComparer(), playerManager);
        return new Game(core, playerManager);
    }
}
