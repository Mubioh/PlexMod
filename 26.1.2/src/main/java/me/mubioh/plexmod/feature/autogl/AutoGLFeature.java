package me.mubioh.plexmod.feature.autogl;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.GameStartEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class AutoGLFeature implements PlexFeature {

    private Consumer<GameStartEvent> listener;

    @Override public String getId()          { return "autogl"; }
    @Override public String getDisplayName() { return "AutoGL"; }
    @Override public String getTooltip()     { return "Automatically sends a good luck message when a game starts."; }

    @Override
    public void onEnable() {
        listener = event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            String message = PlexConfig.getInstance().getAutoGlMessage();
            mc.execute(() -> mc.player.connection.sendChat(message));
        };
        EventBus.subscribe(GameStartEvent.class, listener);
    }

    @Override
    public void onDisable() {
        EventBus.unsubscribe(GameStartEvent.class, listener);
        listener = null;
    }
}
