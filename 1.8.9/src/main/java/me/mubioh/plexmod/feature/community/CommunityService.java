package me.mubioh.plexmod.feature.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.EnumChatFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.function.Consumer;

public class CommunityService {

    private static final Logger LOGGER   = LogManager.getLogger("PlexMod/CommunityService");
    private static final String BASE_URL = "https://plexmod.mubiohs.workers.dev";

    private CommunityService() {}

    public static void fetchByName(final String communityName,
                                   final Consumer<CommunityChannel> onSuccess,
                                   final Runnable onFailure) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    String encoded = communityName.replace(" ", "%20");
                    String body = httpGet(BASE_URL + "/v1/communities?name=" + encoded);
                    JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                    JsonArray entries = json.getAsJsonArray("entries");

                    for (JsonElement el : entries) {
                        JsonObject entry = el.getAsJsonObject();
                        String name = entry.get("name").getAsString();
                        if (name.equalsIgnoreCase(communityName)) {
                            JsonObject fmt = entry.getAsJsonObject("chatFormatting");
                            EnumChatFormatting prefix = parseColor(fmt.get("prefix").getAsString(),     EnumChatFormatting.BLUE);
                            EnumChatFormatting player = parseColor(fmt.get("playerName").getAsString(), EnumChatFormatting.RED);
                            EnumChatFormatting msg    = parseColor(fmt.get("message").getAsString(),    EnumChatFormatting.GREEN);
                            CommunityChannel channel  = new CommunityChannel(name, prefix, player, msg);
                            onSuccess.accept(channel);
                            return;
                        }
                    }
                    LOGGER.warn("[PlexMod] fetchByName: no match for '{}'", communityName);
                    onFailure.run();
                } catch (Exception e) {
                    LOGGER.error("[PlexMod] fetchByName error", e);
                    onFailure.run();
                }
            }
        }, "PlexMod-CommunityFetch");
        t.setDaemon(true);
        t.start();
    }

    public static void fetchForPlayer(final String uuid,
                                      final Consumer<CommunityChannel> onSuccess,
                                      final Runnable onFailure) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    int status = getStatusCode(BASE_URL + "/v1/players/" + uuid + "/community");
                    if (status == 404) { LOGGER.info("[PlexMod] Player not in a community."); onFailure.run(); return; }
                    if (status != 200) { LOGGER.warn("[PlexMod] Community API status: {}", status); onFailure.run(); return; }

                    String body = httpGet(BASE_URL + "/v1/players/" + uuid + "/community");
                    JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                    if (!json.has("community") || json.get("community").isJsonNull()) {
                        onFailure.run(); return;
                    }
                    JsonObject community = json.getAsJsonObject("community");
                    String name = community.get("name").getAsString();
                    JsonObject fmt = community.getAsJsonObject("chatFormatting");
                    EnumChatFormatting prefix = parseColor(fmt.get("prefix").getAsString(),     EnumChatFormatting.BLUE);
                    EnumChatFormatting player = parseColor(fmt.get("playerName").getAsString(), EnumChatFormatting.RED);
                    EnumChatFormatting msg    = parseColor(fmt.get("message").getAsString(),    EnumChatFormatting.GREEN);
                    CommunityChannel channel  = new CommunityChannel(name, prefix, player, msg);
                    LOGGER.info("[PlexMod] Community resolved: {}", name);
                    onSuccess.accept(channel);
                } catch (Exception e) {
                    LOGGER.error("[PlexMod] fetchForPlayer error", e);
                    onFailure.run();
                }
            }
        }, "PlexMod-CommunityPlayerFetch");
        t.setDaemon(true);
        t.start();
    }

    private static int getStatusCode(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("User-Agent", "PlexMod/1.1.2");
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "PlexMod/1.1.2");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static EnumChatFormatting parseColor(String value, EnumChatFormatting fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        if (value.startsWith("§") && value.length() == 2) {
            char code = value.charAt(1);
            for (EnumChatFormatting fmt : EnumChatFormatting.values()) {
                if (fmt.toString().length() == 2 && fmt.toString().charAt(1) == code) return fmt;
            }
            return fallback;
        }
        try { return EnumChatFormatting.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}