package me.mubioh.plexmod.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class GameMetadataService {

    private static final Logger LOGGER   = LoggerFactory.getLogger("PlexMod/GameMetadata");
    private static final String BASE_URL = "https://plexmod.mubiohs.workers.dev";

    private static final Set<String> FORCE_TEAM_GAMES = Set.of(
            "minestrike", "minestrikelegacy", "blockhunt", "blockhuntlegacy"
    );

    private static GameMetadataService instance;
    public static GameMetadataService getInstance() {
        if (instance == null) instance = new GameMetadataService();
        return instance;
    }

    private final Map<String, String>  displayNameToGameType = new ConcurrentHashMap<>();
    private final Map<String, Integer> displayNameToTeamSize = new ConcurrentHashMap<>();

    private GameMetadataService() {
        Thread t = new Thread(this::load, "PlexMod-GameMetadata");
        t.setDaemon(true);
        t.start();
    }

    public String getGameType(String displayName) {
        if (displayName == null) return null;
        String normalized = normalize(displayName);
        String type = displayNameToGameType.get(normalized);
        return type != null ? type : normalized;
    }

    public boolean isTeamGame(String displayName) {
        if (displayName == null) return false;
        String key = normalize(displayName);

        if (FORCE_TEAM_GAMES.contains(key)) return true;

        if (displayNameToTeamSize.containsKey(key)) {
            return displayNameToTeamSize.get(key) > 1;
        }
        Integer withS = displayNameToTeamSize.get(key + "s");
        return withS != null && withS > 1;
    }

    private void load() {
        try {
            URL u = URI.create(BASE_URL + "/v1/references/games/metadata").toURL();
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                LOGGER.warn("[PlexMod] Game metadata returned HTTP {}", status);
                return;
            }

            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                body = br.lines().collect(Collectors.joining("\n"));
            } finally {
                conn.disconnect();
            }

            JsonObject root  = JsonParser.parseString(body).getAsJsonObject();
            JsonArray  games = root.getAsJsonArray("games");

            for (JsonElement el : games) {
                JsonObject game        = el.getAsJsonObject();
                String     gameType    = game.get("gameType").getAsString();
                String     displayName = game.get("displayName").getAsString();
                int        teamSize    = game.get("teamSize").getAsInt();

                String displayKey  = normalize(displayName);
                String gameTypeKey = normalize(gameType);

                displayNameToGameType.put(displayKey,  gameType);
                displayNameToTeamSize.put(displayKey,  teamSize);
                displayNameToGameType.put(gameTypeKey, gameType);
                displayNameToTeamSize.put(gameTypeKey, teamSize);
            }

            LOGGER.info("[PlexMod] Loaded {} game types from metadata.", displayNameToGameType.size());

        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to load game metadata", e);
        }
    }

    private static String normalize(String text) {
        return GameDetectorUtil.normalize(text);
    }
}