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
    public static <T extends Event> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners
                .computeIfAbsent(eventType, k -> new ArrayList<>())
                .add((Consumer) listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Event> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<? super Event>> list = listeners.get(eventType);
        if (list != null) list.remove((Consumer) listener);
    }

    public static <T extends Event> T publish(T event) {
        List<Consumer<? super Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<? super Event> listener : new ArrayList<>(eventListeners)) {
                if (event.isCancellable() && event.isCancelled()) break;
                listener.accept(event);
            }
        }
        return event;
    }

    public static void unsubscribeAll(Class<? extends Event> eventType) {
        listeners.remove(eventType);
    }

    public static void clear() {
        listeners.clear();
    }
}