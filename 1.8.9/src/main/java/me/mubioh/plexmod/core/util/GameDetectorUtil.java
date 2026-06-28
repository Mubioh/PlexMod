package me.mubioh.plexmod.core.util;

import me.mubioh.plexmod.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public final class GameDetectorUtil {

    private static final Logger LOGGER          = LogManager.getLogger("PlexMod/GameDetector");
    private static final String NANO_GAMES      = "Nano Games";
    private static final String NANO_GAMES_TYPE = "nanogames";

    private GameDetectorUtil() {}

    public static Detection detect() {
        String header = readTablistHeader();
        if (header == null || header.isEmpty()) return lobby();

        String cleaned = cleanLine(header);
        if (cleaned == null || isLobbyHeader(cleaned) || isMineplexNetwork(cleaned)) return lobby();

        if (isNanoGames(cleaned)) return new Detection(false, NANO_GAMES, NANO_GAMES_TYPE);

        String gameName = cleanGameName(cleaned);
        if (gameName == null) return lobby();

        String gameType = GameMetadataService.getInstance().getGameType(gameName);
        return new Detection(false, gameName, gameType);
    }

    public static boolean isInLobby()         { return detect().isLobby(); }
    public static String  getCurrentGameName() { return detect().gameName(); }
    public static String  getCurrentGameType() { return detect().gameType(); }

    public static boolean isInTeamGame() {
        String gameName = getCurrentGameName();
        if (gameName == null) return false;
        if (!GameMetadataService.getInstance().isTeamGame(gameName)) return false;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return true;

        // Check hotbar for "Return to Hub" item — if present, we're spectating
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null) continue;
            String plain = stack.getDisplayName()
                    .replaceAll("§[0-9a-fk-or]", "")
                    .trim();
            if (plain.equals("Return to Hub")) return false;
        }
        return true;
    }

    private static Detection lobby() { return new Detection(true, null, null); }

    private static String readTablistHeader() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI == null || mc.ingameGUI.getTabList() == null) return null;

            GuiPlayerTabOverlay tabList = mc.ingameGUI.getTabList();
            IChatComponent hdr = ((PlayerTabOverlayAccessor) tabList).plexmod$getHeader();
            if (hdr == null) return null;

            String full = stripColour(hdr.getUnformattedText()).trim();
            // Split on newlines; return the first non-blank, non-numeric line
            // (numeric-only first line = Minestrike scoreboard)
            for (String line : full.split("[\n\r]+")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.matches("[0-9]+")) return "Minestrike";
                return trimmed;
            }
            return null;
        } catch (Exception e) {
//            LOGGER.warn("[PlexMod] Failed to read tab-list header: " + e.getMessage());
            return null;
        }
    }

    private static boolean isNanoGames(String h)       { return normalize(h).contains("nanogames"); }
    private static boolean isLobbyHeader(String h)     { return normalize(h).contains("lobby"); }
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

    // package-visible so GameMetadataService can reuse
    static String normalize(String s) {
        if (s == null) return "";
        return stripColour(s).toLowerCase(Locale.ROOT)
                .replace("_","").replace(" ","").replace("-","")
                .replace(":","").replace(".","").trim();
    }

    static String stripColour(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public static class Detection {
        private final boolean isLobby;
        private final String  gameName;
        private final String  gameType;
        public Detection(boolean isLobby, String gameName, String gameType) {
            this.isLobby  = isLobby;
            this.gameName = gameName;
            this.gameType = gameType;
        }
        public boolean isLobby()  { return isLobby; }
        public String  gameName() { return gameName; }
        public String  gameType() { return gameType; }
    }
}
