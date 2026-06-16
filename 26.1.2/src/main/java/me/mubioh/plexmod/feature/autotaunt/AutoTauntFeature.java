package me.mubioh.plexmod.feature.autotaunt;

import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.GameStartEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class AutoTauntFeature implements PlexFeature {

    private Consumer<GameStartEvent> listener;

    @Override public String getId()          { return "autotaunt"; }
    @Override public String getDisplayName() { return "AutoTaunt"; }
    @Override public String getTooltip()     { return "Automatically runs /taunt when a game starts."; }

    @Override
    public void onEnable() {
        listener = event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.execute(() -> mc.player.connection.sendCommand("taunt"));
        };
        EventBus.subscribe(GameStartEvent.class, listener);
    }

    @Override
    public void onDisable() {
        EventBus.unsubscribe(GameStartEvent.class, listener);
        listener = null;
    }
}