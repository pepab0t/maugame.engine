package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.game.action.Action;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ActionPublisher {

    void publishActionToAll(Action action);

    void publishActionExcludingPlayer(Action action, String playerId);

    void publishAction(Player player, Action action);

}

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ActionPublisherImpl implements ActionPublisher {

    private final Supplier<Stream<Player>> players;

    private void distributeAction(
            Action action,
            Predicate<Player> playerPredicate
    ) {
        var s = players.get();
        if (playerPredicate != null)
            s = s.filter(playerPredicate);
        s.forEach(player -> player.trigger(action));
    }

    public void publishActionToAll(Action action) {
        distributeAction(action, null);
    }

    public void publishActionExcludingPlayer(Action action, String playerId) {
        distributeAction(action, player -> !playerId.equals(player.getPlayerId()));
    }

    public void publishAction(Player player, Action action) {
        player.trigger(action);
    }
}

class ActionPublisherBuilder {
    private Supplier<Stream<Player>> players;

    public ActionPublisherBuilder withPlayers(Supplier<Stream<Player>> players) {
        this.players = players;
        return this;
    }

    public ActionPublisher build() {
        Objects.requireNonNull(players);
        return new ActionPublisherImpl(players);
    }
}
