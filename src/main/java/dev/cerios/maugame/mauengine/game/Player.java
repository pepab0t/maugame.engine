package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.game.action.Action;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true)
public class Player {
    @Getter
    @ToString.Include
    private final String playerId;
    @Getter
    @ToString.Include
    private final String username;
    @Getter
    private boolean finished = false;
    @Getter(AccessLevel.PACKAGE)
    private final List<Card> hand = new ArrayList<>();
    private final GameEventListener eventListener;
    private final Runnable countDown;

    public Player(String playerId, String username, GameEventListener eventListener) {
        this(playerId, username, eventListener, () -> {});
    }

    Player(String playerId, String username, GameEventListener eventListener, Runnable countDown) {
        this.playerId = playerId;
        this.eventListener = eventListener;
        this.username = username;
        this.countDown = countDown;
    }

    void deactivate() {
        if (!finished) {
            countDown.run();
            finished = true;
        }
    }

    void trigger(Action action) {
        eventListener.accept(this, action);
    }
}
