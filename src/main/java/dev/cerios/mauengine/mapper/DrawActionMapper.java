package dev.cerios.mauengine.mapper;

import dev.cerios.mauengine.game.action.DrawAction;
import dev.cerios.mauengine.game.action.DrawHiddenAction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DrawActionMapper {

    @Mapping(target = "count", expression = "java(source.cardsDrawn().size())")
    DrawHiddenAction getDrawAction(DrawAction source);
}
