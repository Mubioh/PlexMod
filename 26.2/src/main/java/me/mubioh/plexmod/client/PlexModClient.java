package me.mubioh.plexmod.client;

import me.mubioh.plexmod.core.chat.ChatPatternEngine;
import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.feature.PlexRegistry;
import me.mubioh.plexmod.feature.autofriend.AutoFriendFeature;
import me.mubioh.plexmod.feature.autogg.AutoGGFeature;
import me.mubioh.plexmod.feature.autogl.AutoGLFeature;
import me.mubioh.plexmod.feature.autotaunt.AutoTauntFeature;
import me.mubioh.plexmod.feature.betterlobbies.BetterLobbiesFeature;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import me.mubioh.plexmod.feature.discord.DiscordRPCFeature;
import me.mubioh.plexmod.feature.nametag.NameTagFeature;
import me.mubioh.plexmod.keybinds.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class PlexModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("PlexMod");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PlexMod] Starting Mineplex Mod...");

        PlexConfig.getInstance();

        ChatPatternEngine.register();
        KeybindManager.register();

        PlexRegistry.register(new NameTagFeature());
        PlexRegistry.register(new DiscordRPCFeature());

        PlexRegistry.register(new AutoGLFeature());
        PlexRegistry.register(new AutoGGFeature());
        PlexRegistry.register(new AutoTauntFeature());
        PlexRegistry.register(new AutoFriendFeature());
        PlexRegistry.register(new BetterLobbiesFeature());

        PlexRegistry.register(new ChatCycleFeature());

        LOGGER.info("[PlexMod] Mineplex Mod ready.");
    }
}
