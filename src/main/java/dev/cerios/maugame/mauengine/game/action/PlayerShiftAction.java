package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;
import lombok.*;

@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class PlayerShiftAction implements Action {

    private final Player player;
    @EqualsAndHashCode.Exclude
    private final long expireAtMs;

    @Override
    public ActionType getType() {
        return ActionType.PLAYER_SHIFT;
    }
}
