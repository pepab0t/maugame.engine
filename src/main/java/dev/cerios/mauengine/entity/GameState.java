package dev.cerios.mauengine.entity;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.game.Game;
import dev.cerios.mauengine.game.effect.GameEffect;
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
    private final Game.Stage stage;
    private final String currentPlayer;
    private final GameEffect gameEffect;
}
