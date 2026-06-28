package me.mubioh.plexmod.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlexConfig {

    private static final Logger LOGGER    = LoggerFactory.getLogger("PlexMod/Config");
    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mineplexmod.json";

    private static PlexConfig instance;
    public static PlexConfig getInstance() {
        if (instance == null) instance = new PlexConfig();
        return instance;
    }

    private boolean autoGgEnabled        = true;
    private boolean autoGlEnabled        = true;
    private boolean autoTauntEnabled     = false;
    private boolean autoFriendEnabled    = true;
    private boolean discordRpcEnabled    = true;
    private boolean chatCycleEnabled     = true;
    private boolean nametagEnabled       = true;
    private boolean betterLobbiesEnabled = true;
    private boolean scoreboardRedEnabled = true;
    private boolean playerTagEnabled     = true;
    private boolean communityChatEnabled = true;

    private String nametagExtraTag = "NONE";
    private String autoGgMessage   = "GG!";
    private String autoGlMessage   = "GL HF!";

    private PlexConfig() { load(); }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            autoGgEnabled        = getBool(json, "autoGgEnabled",        true);
            autoGlEnabled        = getBool(json, "autoGlEnabled",        true);
            autoTauntEnabled     = getBool(json, "autoTauntEnabled",     false);
            autoFriendEnabled    = getBool(json, "autoFriendEnabled",    true);
            discordRpcEnabled    = getBool(json, "discordRpcEnabled",    true);
            chatCycleEnabled     = getBool(json, "chatCycleEnabled",     true);
            nametagEnabled       = getBool(json, "nametagEnabled",       true);
            playerTagEnabled     = getBool(json, "playerTagEnabled",     true);
            betterLobbiesEnabled = getBool(json, "betterLobbiesEnabled", true);
            scoreboardRedEnabled = getBool(json, "scoreboardRedEnabled", true);
            communityChatEnabled = getBool(json, "communityChatEnabled", true);

            nametagExtraTag = getString(json, "nametagExtraTag", "NONE");
            autoGgMessage   = getString(json, "autoGgMessage",   "GG!");
            autoGlMessage   = getString(json, "autoGlMessage",   "GL HF!");

            LOGGER.info("[PlexMod] Config loaded.");
        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to load config, using defaults: {}", e.getMessage());
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("autoGgEnabled",        autoGgEnabled);
        json.addProperty("autoGlEnabled",        autoGlEnabled);
        json.addProperty("autoTauntEnabled",     autoTauntEnabled);
        json.addProperty("autoFriendEnabled",    autoFriendEnabled);
        json.addProperty("discordRpcEnabled",    discordRpcEnabled);
        json.addProperty("chatCycleEnabled",     chatCycleEnabled);
        json.addProperty("nametagEnabled",       nametagEnabled);
        json.addProperty("playerTagEnabled",     playerTagEnabled);
        json.addProperty("betterLobbiesEnabled", betterLobbiesEnabled);
        json.addProperty("scoreboardRedEnabled", scoreboardRedEnabled);
        json.addProperty("communityChatEnabled", communityChatEnabled);
        json.addProperty("nametagExtraTag",      nametagExtraTag);
        json.addProperty("autoGgMessage",        autoGgMessage);
        json.addProperty("autoGlMessage",        autoGlMessage);

        try (Writer writer = Files.newBufferedWriter(getConfigPath())) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("[PlexMod] Failed to save config: {}", e.getMessage());
        }
    }

    public boolean isFeatureEnabled(String featureId) {
        return switch (featureId) {
            case "autogg"         -> autoGgEnabled;
            case "autogl"         -> autoGlEnabled;
            case "autotaunt"      -> autoTauntEnabled;
            case "autofriend"     -> autoFriendEnabled;
            case "discord_rpc"    -> discordRpcEnabled;
            case "chat_cycle"     -> chatCycleEnabled;
            case "nametag"        -> nametagEnabled;
            case "better_lobbies" -> betterLobbiesEnabled;
            case "scoreboard_red" -> scoreboardRedEnabled;
            case "community_chat" -> communityChatEnabled;
            case "player_tag"     -> playerTagEnabled;
            default -> {
                LOGGER.warn("[PlexMod] Unknown feature ID: {}", featureId);
                yield false;
            }
        };
    }

    public void setFeatureEnabled(String featureId, boolean enabled) {
        switch (featureId) {
            case "autogg"         -> autoGgEnabled        = enabled;
            case "autogl"         -> autoGlEnabled        = enabled;
            case "autotaunt"      -> autoTauntEnabled     = enabled;
            case "autofriend"     -> autoFriendEnabled    = enabled;
            case "discord_rpc"    -> discordRpcEnabled    = enabled;
            case "chat_cycle"     -> chatCycleEnabled     = enabled;
            case "nametag"        -> nametagEnabled       = enabled;
            case "better_lobbies" -> betterLobbiesEnabled = enabled;
            case "player_tag"     -> playerTagEnabled     = enabled;
            case "scoreboard_red" -> scoreboardRedEnabled = enabled;
            case "community_chat" -> communityChatEnabled = enabled;
            default -> {
                LOGGER.warn("[PlexMod] Unknown feature ID: {}", featureId);
                return;
            }
        }
        save();
    }

    public NametagExtraTag getNametagExtraTag() {
        try {
            return NametagExtraTag.valueOf(nametagExtraTag.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NametagExtraTag.NONE;
        }
    }

    public void setNametagExtraTag(NametagExtraTag tag) {
        this.nametagExtraTag = tag.name();
        save();
    }

    public enum NametagExtraTag { NONE, LEVEL }

    public String getAutoGgMessage() { return autoGgMessage; }
    public void   setAutoGgMessage(String m) { this.autoGgMessage = m; save(); }

    public String getAutoGlMessage() { return autoGlMessage; }
    public void   setAutoGlMessage(String m) { this.autoGlMessage = m; save(); }

    private boolean getBool(JsonObject json, String key, boolean def) {
        return json.has(key) ? json.get(key).getAsBoolean() : def;
    }

    private String getString(JsonObject json, String key, String def) {
        return json.has(key) ? json.get(key).getAsString() : def;
    }
}