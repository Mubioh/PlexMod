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
    private boolean betterLobbiesEnabled = true;
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
            betterLobbiesEnabled = bool(j, "betterLobbiesEnabled", true);
            autoGgMessage        = str(j,  "autoGgMessage",        "GG!");
            autoGlMessage        = str(j,  "autoGlMessage",        "GL HF!");
            LOGGER.info("[PlexMod] Config loaded.");
        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to load config, using defaults: {}", e.getMessage());
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
        j.addProperty("betterLobbiesEnabled", betterLobbiesEnabled);
        j.addProperty("autoGgMessage",        autoGgMessage);
        j.addProperty("autoGlMessage",        autoGlMessage);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(getConfigFile()), StandardCharsets.UTF_8)) {
            GSON.toJson(j, w);
        } catch (IOException e) {
            LOGGER.error("[PlexMod] Failed to save config: {}", e.getMessage());
        }
    }

    public boolean isFeatureEnabled(String id) {
        switch (id) {
            case "autogg":         return autoGgEnabled;
            case "autogl":         return autoGlEnabled;
            case "autotaunt":      return autoTauntEnabled;
            case "autofriend":     return autoFriendEnabled;
            case "discord_rpc":    return discordRpcEnabled;
            case "chat_cycle":     return chatCycleEnabled;
            case "nametag":        return nametagEnabled;
            case "better_lobbies": return betterLobbiesEnabled;
            default: LOGGER.warn("[PlexMod] Unknown feature: {}", id); return false;
        }
    }

    public void setFeatureEnabled(String id, boolean val) {
        switch (id) {
            case "autogg":         autoGgEnabled        = val; break;
            case "autogl":         autoGlEnabled        = val; break;
            case "autotaunt":      autoTauntEnabled     = val; break;
            case "autofriend":     autoFriendEnabled    = val; break;
            case "discord_rpc":    discordRpcEnabled    = val; break;
            case "chat_cycle":     chatCycleEnabled     = val; break;
            case "nametag":        nametagEnabled       = val; break;
            case "better_lobbies": betterLobbiesEnabled = val; break;
            default: LOGGER.warn("[PlexMod] Unknown feature: {}", id); return;
        }
        save();
    }

    public String getAutoGgMessage()             { return autoGgMessage; }
    public void   setAutoGgMessage(String msg)   { autoGgMessage = msg; }
    public String getAutoGlMessage()             { return autoGlMessage; }
    public void   setAutoGlMessage(String msg)   { autoGlMessage = msg; }

    private boolean bool(JsonObject j, String k, boolean def) { return j.has(k) ? j.get(k).getAsBoolean() : def; }
    private String  str (JsonObject j, String k, String  def) { return j.has(k) ? j.get(k).getAsString()  : def; }
}
