package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import lombok.Getter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
class Player {
    private final String playerId;

    private boolean active = true;
    private final List<Card> hand = new ArrayList<>();

    public Player(String playerId) {
        this.playerId = playerId;
    }

    public void enable() {
        active = true;
    }

    public void disable() {
        active = false;
    }
}
