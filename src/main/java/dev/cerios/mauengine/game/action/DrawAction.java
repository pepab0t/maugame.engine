package dev.cerios.mauengine.game.action;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.mapper.DrawActionMapper;
import lombok.Getter;
import org.mapstruct.factory.Mappers;

import java.util.List;

public class DrawAction implements Action {

    private final DrawActionMapper mapper = Mappers.getMapper(DrawActionMapper.class);
    @Getter
    private final String playerId;
    @Getter
    private final List<Card> cardsDrawn;

    public DrawAction(String playerId, List<Card> cardsDrawn) {
        this.playerId = playerId;
        this.cardsDrawn = cardsDrawn;
    }

    @Override
    public ActionType type() {
        return ActionType.DRAW;
    }

    @Override
    public PrivateAction hide(String playerId) {
        return mapper.getDrawAction(this);
    }
}
