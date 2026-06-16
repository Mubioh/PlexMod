package me.mubioh.plexmod.feature.autogg;
import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.GameEndEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import net.minecraft.client.Minecraft;
import java.util.function.Consumer;
public class AutoGGFeature implements PlexFeature {
    private Consumer<GameEndEvent> listener;
    @Override public String getId()          { return "autogg"; }
    @Override public String getDisplayName() { return "AutoGG"; }
    @Override public String getTooltip()     { return "Automatically sends a message in chat when a game ends."; }
    @Override public void onEnable() {
        listener = e -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) mc.thePlayer.sendChatMessage(PlexConfig.getInstance().getAutoGgMessage());
        };
        EventBus.subscribe(GameEndEvent.class, listener);
    }
    @Override public void onDisable() { EventBus.unsubscribe(GameEndEvent.class, listener); listener = null; }
}
