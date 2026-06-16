package me.mubioh.plexmod.core.util;

import net.minecraft.client.Minecraft;

public class MineplexHelper {

    private static final String DOMAIN = "mineplex.com";

    private MineplexHelper() {}

    public static boolean isOnMineplex() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getCurrentServerData() == null) return false;
        String ip = mc.getCurrentServerData().serverIP.toLowerCase().trim();
        return ip.equals(DOMAIN)
            || ip.startsWith(DOMAIN + ":")
            || ip.endsWith("." + DOMAIN)
            || ip.contains("." + DOMAIN + ":");
    }

    public static boolean isInMultiplayer() {
        return Minecraft.getMinecraft().getCurrentServerData() != null;
    }
}
