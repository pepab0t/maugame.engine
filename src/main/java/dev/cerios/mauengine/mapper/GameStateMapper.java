package dev.cerios.mauengine.mapper;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.exception.MauEngineBaseException;
import dev.cerios.mauengine.entity.GameState;
import dev.cerios.mauengine.entity.GameStateHidden;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface GameStateMapper {
    @Mapping(target = "playerCards", source = "source.playerHands", qualifiedByName = "getPlayerIdFromMap")
    @Mapping(target = "playerHands", source = "source.playerHands", qualifiedByName = "hidePlayerCards")
    GameStateHidden toHidden(GameState source, @Context String playerId) throws MauEngineBaseException;

    @Named("getPlayerIdFromMap")
    default List<Card> getPlayerIdFromMap(Map<String, List<Card>> playerHands, @Context String playerId) throws MauEngineBaseException {
        List<Card> playerCards = playerHands.get(playerId);
        if (playerCards == null)
            throw new MauEngineBaseException("Player " + playerId + " not found");
        return playerCards;
    }

    @Named("hidePlayerCards")
    default Map<String, Integer> hidePlayerCards(Map<String, List<Card>> playerHands, @Context String playerId) throws MauEngineBaseException {
        return playerHands.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(playerId))
                .map(e -> Map.entry(e.getKey(), e.getValue().size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
