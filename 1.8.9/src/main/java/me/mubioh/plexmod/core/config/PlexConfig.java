package me.mubioh.plexmod.core.config;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PlexConfig {

    private static final Logger LOGGER    = LogManager.getLogger("PlexMod/Config");
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
    private boolean playerTagEnabled     = true;
    private boolean betterLobbiesEnabled = true;
    private boolean scoreboardRedEnabled = true;
    private boolean communityChatEnabled = true;
    private String  nametagExtraTag      = "NONE";
    private String  autoGgMessage        = "GG!";
    private String  autoGlMessage        = "GL HF!";

    private PlexConfig() { load(); }

    private File getConfigFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, FILE_NAME);
    }

    public void load() {
        File file = getConfigFile();
        if (!file.exists()) { LOGGER.info("[PlexMod] No config found, writing defaults."); save(); return; }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject j = new JsonParser().parse(r).getAsJsonObject();
            autoGgEnabled        = bool(j, "autoGgEnabled",        true);
            autoGlEnabled        = bool(j, "autoGlEnabled",        true);
            autoTauntEnabled     = bool(j, "autoTauntEnabled",     false);
            autoFriendEnabled    = bool(j, "autoFriendEnabled",    true);
            discordRpcEnabled    = bool(j, "discordRpcEnabled",    true);
            chatCycleEnabled     = bool(j, "chatCycleEnabled",     true);
            nametagEnabled       = bool(j, "nametagEnabled",       true);
            playerTagEnabled     = bool(j, "playerTagEnabled",     true);
            betterLobbiesEnabled = bool(j, "betterLobbiesEnabled", true);
            scoreboardRedEnabled = bool(j, "scoreboardRedEnabled", true);
            communityChatEnabled = bool(j, "communityChatEnabled", true);
            nametagExtraTag      = str(j,  "nametagExtraTag",      "NONE");
            autoGgMessage        = str(j,  "autoGgMessage",        "GG!");
            autoGlMessage        = str(j,  "autoGlMessage",        "GL HF!");
            LOGGER.info("[PlexMod] Config loaded.");
        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to load config: {}", e.getMessage());
        }
    }

    public void save() {
        JsonObject j = new JsonObject();
        j.addProperty("autoGgEnabled",        autoGgEnabled);
        j.addProperty("autoGlEnabled",        autoGlEnabled);
        j.addProperty("autoTauntEnabled",     autoTauntEnabled);
        j.addProperty("autoFriendEnabled",    autoFriendEnabled);
        j.addProperty("discordRpcEnabled",    discordRpcEnabled);
        j.addProperty("chatCycleEnabled",     chatCycleEnabled);
        j.addProperty("nametagEnabled",       nametagEnabled);
        j.addProperty("playerTagEnabled",     playerTagEnabled);
        j.addProperty("betterLobbiesEnabled", betterLobbiesEnabled);
        j.addProperty("scoreboardRedEnabled", scoreboardRedEnabled);
        j.addProperty("communityChatEnabled", communityChatEnabled);
        j.addProperty("nametagExtraTag",      nametagExtraTag);
        j.addProperty("autoGgMessage",        autoGgMessage);
        j.addProperty("autoGlMessage",        autoGlMessage);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(getConfigFile()), StandardCharsets.UTF_8)) {
            GSON.toJson(j, w);
        } catch (IOException e) {
            LOGGER.error("[PlexMod] Failed to save config: {}", e.getMessage());
        }
    }

    public boolean isFeatureEnabled(String id) {
        if ("autogg".equals(id))         return autoGgEnabled;
        if ("autogl".equals(id))         return autoGlEnabled;
        if ("autotaunt".equals(id))      return autoTauntEnabled;
        if ("autofriend".equals(id))     return autoFriendEnabled;
        if ("discord_rpc".equals(id))    return discordRpcEnabled;
        if ("chat_cycle".equals(id))     return chatCycleEnabled;
        if ("nametag".equals(id))        return nametagEnabled;
        if ("player_tag".equals(id))     return playerTagEnabled;
        if ("better_lobbies".equals(id)) return betterLobbiesEnabled;
        if ("scoreboard_red".equals(id)) return scoreboardRedEnabled;
        if ("community_chat".equals(id)) return communityChatEnabled;
        LOGGER.warn("[PlexMod] Unknown feature ID: {}", id);
        return false;
    }

    public void setFeatureEnabled(String id, boolean val) {
        if ("autogg".equals(id))         { autoGgEnabled        = val; }
        else if ("autogl".equals(id))    { autoGlEnabled        = val; }
        else if ("autotaunt".equals(id)) { autoTauntEnabled     = val; }
        else if ("autofriend".equals(id)){ autoFriendEnabled    = val; }
        else if ("discord_rpc".equals(id)){ discordRpcEnabled   = val; }
        else if ("chat_cycle".equals(id)){ chatCycleEnabled     = val; }
        else if ("nametag".equals(id))   { nametagEnabled       = val; }
        else if ("player_tag".equals(id)){ playerTagEnabled     = val; }
        else if ("better_lobbies".equals(id)){ betterLobbiesEnabled = val; }
        else if ("scoreboard_red".equals(id)){ scoreboardRedEnabled = val; }
        else if ("community_chat".equals(id)){ communityChatEnabled = val; }
        else { LOGGER.warn("[PlexMod] Unknown feature ID: {}", id); return; }
        save();
    }

    public NametagExtraTag getNametagExtraTag() {
        try { return NametagExtraTag.valueOf(nametagExtraTag.toUpperCase()); }
        catch (IllegalArgumentException e) { return NametagExtraTag.NONE; }
    }

    public void setNametagExtraTag(NametagExtraTag tag) {
        this.nametagExtraTag = tag.name();
        save();
    }

    public enum NametagExtraTag { NONE, LEVEL }

    public String getAutoGgMessage()           { return autoGgMessage; }
    public void   setAutoGgMessage(String m)   { autoGgMessage = m; save(); }
    public String getAutoGlMessage()           { return autoGlMessage; }
    public void   setAutoGlMessage(String m)   { autoGlMessage = m; save(); }

    private boolean bool(JsonObject j, String k, boolean def) { return j.has(k) ? j.get(k).getAsBoolean() : def; }
    private String  str (JsonObject j, String k, String  def) { return j.has(k) ? j.get(k).getAsString()  : def; }
}
