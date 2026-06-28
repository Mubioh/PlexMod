package me.mubioh.plexmod.keybinds;

import me.mubioh.plexmod.mixin.KeyMappingCategoryAccessor;
import me.mubioh.plexmod.screen.PlexHudScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KeybindManager {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.parse("mineplexmod"));

    public static KeyMapping openMenu;

    public static void register() {
        moveMineplexCategoryToTop();

        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mineplexmod.open_menu",
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::onTick);
    }

    private static void moveMineplexCategoryToTop() {
        List<KeyMapping.Category> sortOrder = KeyMappingCategoryAccessor.mineplexmod$getSortOrder();
        sortOrder.remove(CATEGORY);
        sortOrder.add(0, CATEGORY);
    }

    private static void onTick(Minecraft client) {
        while (openMenu.consumeClick()) {
            if (client.gui.screen() instanceof PlexHudScreen) return;
            client.gui.setScreen(new PlexHudScreen(client.gui.screen()));
        }
    }
}
