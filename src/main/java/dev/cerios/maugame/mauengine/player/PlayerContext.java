package dev.cerios.maugame.mauengine.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerContext {
    private final PlayerStateFactory factory;

    @Getter
    private PlayerStorage players;
    private Consumer<Player> timeoutListener;
    private Consumer<UUID> startListener;

    public PlayerContext(PlayerStateFactory factory) {
        this.factory = factory;
        this.setLobbyState();
    }

    public void listenPlayerTimeout(Consumer<Player> listener) {
        timeoutListener = listener;
    }

    public void listenStartGame(Consumer<UUID> startListener) {
        this.startListener = startListener;
    }

    public void setLobbyState() {
        var state = factory.createLobbyState(this::setRunningState);
        if (startListener != null)
            state.listenStart(startListener);
        else throw new RuntimeException("Wrong setup, no start listener.");
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
            if (startListener != null)
                finish.listenStart(startListener);
            else throw new RuntimeException("Wrong setup, no start listener.");
            players = finish;
        } else {
            throw new RuntimeException(String.format("Invalid state `%s` for transition to finish state.", players.getClass().getSimpleName()));
        }
    }
}
