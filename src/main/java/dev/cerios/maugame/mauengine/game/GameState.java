package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.effect.GameEffect;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GameState {
    private final List<String> players;
    private final List<String> playerRanks;
    private final Map<String, List<Card>> playerHands;
    private final Card topPile;
    private final int deckSize;
    private final GameCore.Stage stage;
    private final String currentPlayer;
    private final GameEffect gameEffect;
}
