package dev.cerios.maugame.mauengine.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerContext {
    private final PlayerStateFactory factory;

    @Getter
    private PlayerStorage players;
    private Consumer<Player> timeoutListener;
    private final List<Consumer<UUID>> startListeners = new LinkedList<>();

    public PlayerContext(PlayerStateFactory factory) {
        this.factory = factory;
    }

    public void listenPlayerTimeout(Consumer<Player> listener) {
        timeoutListener = listener;
    }

    public void listenStartGame(Consumer<UUID> startListener) {
        this.startListeners.add(startListener);
    }

    public void setLobbyState() {
        var state = factory.createLobbyState(this::setRunningState);
        state.listenStart(startListeners);
        if (startListeners.isEmpty()) throw new RuntimeException("Wrong setup, no start listener.");
        players = state;
    }

    public void setRunningState(Collection<Player> playerCollection) {
        if (players instanceof PlayerReadyStorage) {
            var state = factory.createRunningState(playerCollection, this::setFinishState);
            if (timeoutListener != null)
                state.listenTimeout(timeoutListener);
            players = state;
        } else {
            throw new RuntimeException(String.format("Invalid state `%s` for transition to running state.", players.getClass().getSimpleName()));
        }
    }

    public void setFinishState(Collection<Player> playerCollection) {
        if (players instanceof PlayerRunningState) {
            var finish = factory.createFinishState(playerCollection, this::setRunningState);
            finish.listenStart(startListeners);
            if (startListeners.isEmpty()) throw new RuntimeException("Wrong setup, no start listener.");
            players = finish;
        } else {
            throw new RuntimeException(String.format("Invalid state `%s` for transition to finish state.", players.getClass().getSimpleName()));
        }
    }
}
