package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.action.Action;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Player {
    @Getter
    private final String playerId;
    @Getter
    private boolean active = true;
    @Getter
    private final List<Card> hand = new ArrayList<>();
    private final GameEventListener eventListener;

    public Player(String playerId, GameEventListener eventListener) {
        this.playerId = playerId;
        this.eventListener = eventListener;
    }

    public void enable() {
        active = true;
    }

    public void disable() {
        active = false;
    }

    public void trigger(Action action) {
        eventListener.accept(playerId, action);
    }
}
