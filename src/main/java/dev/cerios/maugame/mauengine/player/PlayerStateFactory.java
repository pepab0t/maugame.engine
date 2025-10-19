package dev.cerios.maugame.mauengine.player;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class PlayerStateFactory {
    private final UUID gameId;
    private final int minPlayers;
    private final int maxPlayers;
    private final Random random;
    private final ReadWriteLock globalLock;
    private final long turnTimeoutMs;

    public PlayerRunningState createRunningState(Collection<Player> players, Consumer<Collection<Player>> stateSwitcher) {
        return new PlayerRunningState(
                random,
                players,
                turnTimeoutMs,
                stateSwitcher,
                globalLock,
                new ActionPublisherBuilder()
        );
    }

    public PlayerLobbyState createLobbyState(Consumer<Collection<Player>> stateSwitcher) {
        return new PlayerLobbyState(
                minPlayers,
                maxPlayers,
                gameId,
                stateSwitcher,
                new ActionPublisherBuilder()
        );
    }

    public PlayerFinishState createFinishState(Collection<Player> players, Consumer<Collection<Player>> stateSwitcher) {
        return new PlayerFinishState(
                players,
                minPlayers,
                gameId,
                new ActionPublisherBuilder(),
                stateSwitcher
        );
    }
}
