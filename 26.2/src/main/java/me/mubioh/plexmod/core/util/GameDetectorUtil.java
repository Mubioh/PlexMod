package me.mubioh.plexmod.core.util;

import me.mubioh.plexmod.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class GameDetectorUtil {

    private static final String NANO_GAMES      = "Nano Games";
    private static final String NANO_GAMES_TYPE = "nanogames";

    private GameDetectorUtil() {}

    public static Detection detect() {
        String header = readTablistHeader();

        if (header == null || header.isBlank()) return lobby();

        String cleanedHeader = cleanLine(header);

        if (cleanedHeader == null || isLobbyHeader(cleanedHeader) || isMineplexNetwork(cleanedHeader)) {
            return lobby();
        }

        if (isNanoGames(cleanedHeader)) {
            return new Detection(false, NANO_GAMES, NANO_GAMES_TYPE);
        }

        String gameName = cleanGameName(cleanedHeader);
        if (gameName == null) return lobby();

        String gameType = GameMetadataService.getInstance().getGameType(gameName);
        return new Detection(false, gameName, gameType);
    }

    public static boolean isInLobby() {
        return detect().isLobby();
    }

    public static String getCurrentGameName() {
        return detect().gameName();
    }

    public static String getCurrentGameType() {
        return detect().gameType();
    }

    public static boolean isInTeamGame() {
        String gameName = getCurrentGameName();
        if (gameName == null) return false;
        if (!GameMetadataService.getInstance().isTeamGame(gameName)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;

        net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            net.minecraft.world.item.ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            String plain = stack.getHoverName().getString()
                    .replaceAll("§[0-9a-fk-or]", "")
                    .trim();
            if (plain.equals("Return to Hub")) return false;
        }

        return true;
    }

    private static Detection lobby() {
        return new Detection(true, null, null);
    }

    private static boolean isNanoGames(String header) {
        return normalize(header).contains("nanogames");
    }

    private static String readTablistHeader() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null || mc.gui.hud.getTabList() == null) return null;

        Component header = ((PlayerTabOverlayAccessor) mc.gui.hud.getTabList()).plexmod$getHeader();
        if (header == null) return null;

        String full = stripColourCodes(header.getString()).trim();
        for (String line : full.split("[\n\r]+")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.matches("[0-9]+")) return "Minestrike";
            return trimmed;
        }

        return null;
    }

    private static boolean isLobbyHeader(String header) {
        return normalize(header).contains("lobby");
    }

    private static boolean isMineplexNetwork(String header) {
        return normalize(header).contains("mineplexnetwork");
    }

    private static String cleanLine(String line) {
        if (line == null) return null;
        String cleaned = stripColourCodes(line).replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String cleanGameName(String gameName) {
        if (gameName == null) return null;
        String cleaned = stripColourCodes(gameName)
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    static String normalize(String text) {
        if (text == null) return "";
        return stripColourCodes(text)
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace(" ", "")
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .trim();
    }

    static String stripColourCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public record Detection(boolean isLobby, String gameName, String gameType) {}
}