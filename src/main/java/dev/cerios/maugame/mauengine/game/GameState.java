package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.effect.GameEffect;

import java.util.List;
import java.util.Map;

public record GameState(
        List<String> playerRank,
        Map<String, List<Card>> playerHands,
        Card topPile,
        int deckSize,
        Stage stage,
        String currentPlayer,
        GameEffect gameEffect
) {
}
