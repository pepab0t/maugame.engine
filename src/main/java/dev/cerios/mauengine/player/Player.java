package dev.cerios.mauengine.player;

public interface Player {

    String getId();

    void doAction(String action);

    void notifyPlayer(Object event);
}
