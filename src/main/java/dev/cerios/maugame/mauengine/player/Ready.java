package dev.cerios.maugame.mauengine.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class Ready {
    private boolean ready = false;
    @Getter
    private final Player player;

    public void set(boolean ready) {
        this.ready = ready;
    }

    public boolean get() {
        return ready;
    }
}
