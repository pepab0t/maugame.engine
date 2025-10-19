package dev.cerios.maugame.mauengine.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class Ready {
    private boolean ready = false;
    @Getter
    private final Player player;

    public boolean set(boolean ready) {
        var changed = ready != this.ready;
        this.ready = ready;
        return changed;
    }

    public boolean get() {
        return ready;
    }
}
