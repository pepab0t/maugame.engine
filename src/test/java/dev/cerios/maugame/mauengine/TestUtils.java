package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.Player;
import dev.cerios.maugame.mauengine.game.action.Action;
import lombok.Getter;

import java.util.*;

public class TestUtils {

    public final static GameEventListener VOID_LISTENER = (p, e) -> {};

    public static Object getField(Object object, String fieldName) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    public static <T> void setField(T obj, String fieldName, Object value) throws Exception {
        var fieldToSet = obj.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(obj, value);
    }

    public static ActionCollector createCollector() {
        return new ActionCollector();
    }

    public static class ActionCollector {
        private final Map<Player, List<Action>> actions = new HashMap<>();
        @Getter
        private final GameEventListener listener = (p, e) -> {
            var playerActions = actions.putIfAbsent(p, new LinkedList<>());
            if (playerActions == null) playerActions = actions.get(p);
            playerActions.add(e);
        };

        private ActionCollector() {}

        public List<Action> getActions(Player p) {
            return actions.getOrDefault(p, Collections.emptyList());
        }

        public void clear() {
            actions.clear();
        }
    }
}
