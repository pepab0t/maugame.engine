package dev.cerios.maugame.mauengine.game;

import com.github.f4b6a3.ulid.UlidCreator;

public class PlayerIdGenerator {
    public static String generatePlayerId() {
        return UlidCreator.getUlid().toString();
    }
}
