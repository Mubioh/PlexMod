package me.mubioh.plexmod.core.util;

import me.mubioh.plexmod.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public final class GameDetectorUtil {

    private static final Logger LOGGER     = LogManager.getLogger("PlexMod/GameDetector");
    private static final String NANO_GAMES = "Nano Games";

    private GameDetectorUtil() {}

    public static Detection detect() {
        String header = readTablistHeader();

        if (header != null && !header.isEmpty()) {
            LOGGER.info("[PlexMod Debug] Active Tab Header Content: '{}'", header);
        }

        if (header == null || header.isEmpty()) return lobby();

        String cleaned = cleanLine(header);
        if (cleaned == null || isLobbyHeader(cleaned) || isMineplexNetwork(cleaned)) return lobby();
        if (isNanoGames(cleaned)) return new Detection(false, NANO_GAMES);

        String gameName = cleanGameName(cleaned);
        return gameName == null ? lobby() : new Detection(false, gameName);
    }

    public static boolean isInLobby()          { return detect().isLobby(); }
    public static String  getCurrentGameName()  { return detect().gameName(); }

    private static Detection lobby() { return new Detection(true, null); }

    private static String readTablistHeader() {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            if (mc.ingameGUI == null || mc.ingameGUI.getTabList() == null) {
                return null;
            }

            GuiPlayerTabOverlay tabList = mc.ingameGUI.getTabList();

            IChatComponent hdr = ((PlayerTabOverlayAccessor) tabList).plexmod$getHeader();

            if (hdr == null) return null;
            return stripColour(hdr.getUnformattedText()).trim();
        } catch (Exception e) {
            LOGGER.warn("[PlexMod] Failed to read tab-list contents via Mixin: " + e.getMessage());
            return null;
        }
    }

    private static boolean isNanoGames(String h)        { return normalize(h).contains("nanogames"); }
    private static boolean isLobbyHeader(String h)      { return normalize(h).contains("lobby"); }
    private static boolean isMineplexNetwork(String h)  { return normalize(h).contains("mineplexnetwork"); }

    private static String cleanLine(String s) {
        if (s == null) return null;
        String c = stripColour(s).replaceAll("\\s+", " ").trim();
        return c.isEmpty() ? null : c;
    }

    private static String cleanGameName(String s) {
        if (s == null) return null;
        String c = stripColour(s).replace("_", " ").replaceAll("\\s+", " ").trim();
        return c.isEmpty() ? null : c;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return stripColour(s).toLowerCase(Locale.ROOT)
                .replace("_","").replace(" ","").replace("-","")
                .replace(":","").replace(".","").trim();
    }

    private static String stripColour(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public static class Detection {
        private final boolean isLobby;
        private final String  gameName;
        public Detection(boolean isLobby, String gameName) { this.isLobby = isLobby; this.gameName = gameName; }
        public boolean isLobby()  { return isLobby; }
        public String  gameName() { return gameName; }
    }
}