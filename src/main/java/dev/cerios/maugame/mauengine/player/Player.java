package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.card.Card;
import lombok.Getter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class Player {
    private final String playerId;
    private final WeakReference<Consumer<Boolean>> activationListener;

    private boolean active = true;
    private final List<Card> hand = new ArrayList<>();

    public Player(String playerId, Consumer<Boolean> activationListener) {
        this.playerId = playerId;
        this.activationListener = new WeakReference<>(activationListener);
    }

    public void enable() {
        active = true;
        tryPublish(true);
    }

    public void disable() {
        active = false;
        tryPublish(false);
    }

    private void tryPublish(boolean value) {
        Consumer<Boolean> listener;
        if ((listener = activationListener.get()) != null) {
            listener.accept(value);
        }
    }
}
