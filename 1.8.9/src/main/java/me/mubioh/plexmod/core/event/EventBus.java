package me.mubioh.plexmod.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventBus {

    private static final Map<Class<? extends Event>, List<Consumer<? super Event>>> listeners
            = new ConcurrentHashMap<>();

    private EventBus() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Event> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add((Consumer) listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Event> void unsubscribe(Class<T> type, Consumer<T> listener) {
        List<Consumer<? super Event>> list = listeners.get(type);
        if (list != null) list.remove((Consumer) listener);
    }

    public static <T extends Event> T publish(T event) {
        List<Consumer<? super Event>> list = listeners.get(event.getClass());
        if (list != null)
            for (Consumer<? super Event> l : new ArrayList<>(list)) {
                if (event.isCancellable() && event.isCancelled()) break;
                l.accept(event);
            }
        return event;
    }

    public static void clear() { listeners.clear(); }
}
