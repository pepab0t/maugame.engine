package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.move.DrawMove;
import dev.cerios.maugame.mauengine.game.move.PassMove;
import dev.cerios.maugame.mauengine.game.move.PlayCardMove;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class Game {
    private final GameCore core;

    public PlayCardMove createPlayMove(final String playerId, Card cardToPlay, Optional<Color> nextColor) {
        return nextColor
                .map(color -> new PlayCardMove(core, playerId, cardToPlay, color))
                .orElseGet(() -> new PlayCardMove(core, playerId, cardToPlay));
    }

    public DrawMove createDrawMove(final String playerId, int count) {
        return new DrawMove(core, playerId, count);
    }

    public PassMove createPassMove(final String playerId) {
        return new PassMove(core, playerId);
    }

    public void registerPlayer(final String playerId) throws GameException {
        core.registerPlayer(playerId);
    }
}
