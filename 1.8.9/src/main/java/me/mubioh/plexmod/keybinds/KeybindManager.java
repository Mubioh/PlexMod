package me.mubioh.plexmod.keybinds;

import me.mubioh.plexmod.screen.PlexScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class KeybindManager {

    public static final KeybindManager INSTANCE = new KeybindManager();

    private static final String CATEGORY = "key.category.mineplexmod";

    public static KeyBinding openMenu;

    private KeybindManager() {}

    public static void register() {
        openMenu  = new KeyBinding("key.mineplexmod.open_menu",   Keyboard.KEY_K,   CATEGORY);
        ClientRegistry.registerKeyBinding(openMenu);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (openMenu.isPressed()) {
            if (mc.currentScreen instanceof PlexScreen) return;
            mc.displayGuiScreen(new PlexScreen(mc.currentScreen));
        }
    }
}
