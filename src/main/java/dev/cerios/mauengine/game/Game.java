package dev.cerios.mauengine.game;

import dev.cerios.mauengine.game.move.DrawMove;
import dev.cerios.mauengine.game.move.PassMove;
import dev.cerios.mauengine.game.move.PlayCardMove;
import dev.cerios.mauengine.game.move.PlayerMove;
import dev.cerios.mauengine.game.move.dto.DrawDto;
import dev.cerios.mauengine.game.move.dto.MoveDto;
import dev.cerios.mauengine.game.move.dto.PassDto;
import dev.cerios.mauengine.game.move.dto.PlayDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Game {
    private final GameCore core;



    public PlayerMove createMove(MoveDto dto) {
        return switch (dto) {
            case PlayDto play -> new PlayCardMove(core, play.getPlayerId(), play.getCard(), play.getNextColor());
            case DrawDto draw -> new DrawMove(core, draw.playerId(), draw.count());
            case PassDto pass -> new PassMove(core, pass.playerId());
        };
    }
}
