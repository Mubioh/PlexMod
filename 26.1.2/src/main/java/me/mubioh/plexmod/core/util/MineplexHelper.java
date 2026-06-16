package me.mubioh.plexmod.core.util;

import net.minecraft.client.Minecraft;

public class MineplexHelper {

    private static final String MINEPLEX_DOMAIN = "mineplex.com";

    private MineplexHelper() {}

    public static boolean isOnMineplex() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getCurrentServer() == null) return false;
        String address = mc.getCurrentServer().ip.toLowerCase().trim();

        if (address.equals(MINEPLEX_DOMAIN) || address.startsWith(MINEPLEX_DOMAIN + ":")) {
            return true;
        }

        if (address.endsWith("." + MINEPLEX_DOMAIN) || address.contains("." + MINEPLEX_DOMAIN + ":")) {
            return true;
        }

        return false;
    }

    public static boolean isInMultiplayer() {
        return Minecraft.getInstance().getCurrentServer() != null;
    }
}