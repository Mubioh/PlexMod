package me.mubioh.plexmod.core.util;

import me.mubioh.plexmod.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class GameDetectorUtil {

    private static final String NANO_GAMES = "Nano Games";

    private GameDetectorUtil() {}

    public static Detection detect() {
        String header = readTablistHeader();

        if (header == null || header.isBlank()) {
            return lobby();
        }

        String cleanedHeader = cleanLine(header);

        if (cleanedHeader == null || isLobbyHeader(cleanedHeader) || isMineplexNetwork(cleanedHeader)) {
            return lobby();
        }

        if (isNanoGames(cleanedHeader)) {
            return new Detection(false, NANO_GAMES);
        }

        String gameName = cleanGameName(cleanedHeader);

        if (gameName == null) {
            return lobby();
        }

        return new Detection(false, gameName);
    }

    public static boolean isInLobby() {
        return detect().isLobby();
    }

    public static String getCurrentGameName() {
        return detect().gameName();
    }

    private static Detection lobby() {
        return new Detection(true, null);
    }

    private static boolean isNanoGames(String header) {
        return normalize(header).contains("nanogames");
    }

    private static String readTablistHeader() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.gui == null || mc.gui.getTabList() == null) {
            return null;
        }

        Component header = ((PlayerTabOverlayAccessor) mc.gui.getTabList()).plexmod$getHeader();

        if (header == null) {
            return null;
        }

        return stripColourCodes(header.getString()).trim();
    }

    private static boolean isLobbyHeader(String header) {
        return normalize(header).contains("lobby");
    }

    private static boolean isMineplexNetwork(String header) {
        return normalize(header).contains("mineplexnetwork");
    }

    private static String cleanLine(String line) {
        if (line == null) return null;

        String cleaned = stripColourCodes(line)
                .replaceAll("\\s+", " ")
                .trim();

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

    private static String normalize(String text) {
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

    private static String stripColourCodes(String text) {
        if (text == null) return "";

        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public record Detection(
            boolean isLobby,
            String gameName
    ) {}
}