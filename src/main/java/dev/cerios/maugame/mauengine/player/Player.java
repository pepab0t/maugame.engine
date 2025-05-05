package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.card.Card;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class Player {
    private final String playerId;
    private boolean active = true;
    private final List<Card> hand = new ArrayList<>();

    void enable() {
        this.active = true;
    }

    void disable() {
        this.active = false;
    }
}
