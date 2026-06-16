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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = PlexModClient.MODID, name = "PlexMod", version = "1.0.0", clientSideOnly = true)
public class PlexModClient {

    public static final String MODID  = "plexmod";
    public static final Logger LOGGER = LogManager.getLogger("PlexMod");

    @Mod.Instance
    public static PlexModClient INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PlexConfig.getInstance();
        KeybindManager.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[PlexMod] Starting Mineplex Mod...");

        ChatPatternEngine.register();

        MinecraftForge.EVENT_BUS.register(KeybindManager.INSTANCE);

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
