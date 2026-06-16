package me.mubioh.plexmod.feature.autofriend;

import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.FriendRequestEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class AutoFriendFeature implements PlexFeature {

    private Consumer<FriendRequestEvent> listener;

    @Override public String getId()          { return "autofriend"; }
    @Override public String getDisplayName() { return "AutoFriend"; }
    @Override public String getTooltip()     { return "Automatically accepts all incoming friend requests."; }

    @Override
    public void onEnable() {
        listener = event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            String command = "friend " + event.getSenderName();
            mc.execute(() -> mc.player.connection.sendCommand(command));
        };
        EventBus.subscribe(FriendRequestEvent.class, listener);
    }

    @Override
    public void onDisable() {
        EventBus.unsubscribe(FriendRequestEvent.class, listener);
        listener = null;
    }
}