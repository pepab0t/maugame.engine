package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.action.Action;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@ToString(onlyExplicitlyIncluded = true)
public class Player {
    @Getter
    private final String playerId;
    @Getter
    @ToString.Include
    private final String username;
    @Getter
    @ToString.Include
    private boolean active = true;
    @Getter
    private final List<Card> hand = new ArrayList<>();
    private final GameEventListener eventListener;

    Player(String playerId, String username, GameEventListener eventListener) {
        this.playerId = playerId;
        this.eventListener = eventListener;
        this.username = username;
    }

    void enable() {
        active = true;
    }

    void disable() {
        active = false;
    }

    void trigger(Action action) {
        eventListener.accept(this, action);
    }
}
