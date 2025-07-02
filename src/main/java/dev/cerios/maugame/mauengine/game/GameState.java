package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.effect.GameEffect;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GameState(
        List<String> playerRank,
        Map<String, List<Card>> playerHands,
        Card topPile,
        int deckSize,
        Stage stage,
        String currentPlayer,
        GameEffect gameEffect
) {
    @Override
    public String toString() {
        var ph = playerHands.entrySet()
                .stream()
                .map(entry -> "\n" + entry.getKey() + ": \n" +
                        entry.getValue().stream().map(Card::toString).collect(Collectors.joining("\n")))
                .collect(Collectors.joining("")) + '\n';
        return "GameState{" +
                "playerRank=" + playerRank +
                ", playerHands=" + ph +
                ", topPile=" + topPile +
                ", deckSize=" + deckSize +
                ", stage=" + stage +
                ", currentPlayer='" + currentPlayer + '\'' +
                ", gameEffect=" + gameEffect +
                '}';
    }
}
