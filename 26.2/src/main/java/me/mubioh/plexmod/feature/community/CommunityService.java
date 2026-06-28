package me.mubioh.plexmod.feature.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Consumer;

public class CommunityService {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/CommunityService");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String BASE_URL = "https://plexmod.mubiohs.workers.dev";

    public static void fetchByName(String communityName, Consumer<CommunityChannel> onSuccess, Runnable onFailure) {
        try {
            String encodedName = URLEncoder.encode(communityName, StandardCharsets.UTF_8);
            String url = BASE_URL + "/v1/communities?name=" + encodedName;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            LOGGER.warn("[PlexMod] fetchByName returned status {}", response.statusCode());
                            onFailure.run();
                            return;
                        }

                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            JsonArray entries = json.getAsJsonArray("entries");

                            for (JsonElement element : entries) {
                                JsonObject entry = element.getAsJsonObject();
                                String name = entry.get("name").getAsString();

                                if (name.equalsIgnoreCase(communityName)) {
                                    JsonObject formatting = entry.getAsJsonObject("chatFormatting");

                                    ChatFormatting prefixColor = parseColor(formatting.get("prefix").getAsString(), ChatFormatting.BLUE);
                                    ChatFormatting nameColor   = parseColor(formatting.get("playerName").getAsString(), ChatFormatting.RED);
                                    ChatFormatting msgColor    = parseColor(formatting.get("message").getAsString(), ChatFormatting.GREEN);

                                    CommunityChannel channel = new CommunityChannel(name);
                                    channel.setFormattingStyles(prefixColor, nameColor, msgColor);

                                    onSuccess.accept(channel);
                                    return;
                                }
                            }
                            LOGGER.warn("[PlexMod] fetchByName: no match found for '{}'", communityName);
                            onFailure.run();
                        } catch (Exception e) {
                            LOGGER.error("[PlexMod] Failed to parse community name list payload", e);
                            onFailure.run();
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("[PlexMod] HTTP error during fetchByName", ex);
                        onFailure.run();
                        return null;
                    });

        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to build fetchByName request for: {}", communityName, e);
            onFailure.run();
        }
    }

    public static void fetchForPlayer(String uuid, Consumer<CommunityChannel> onSuccess, Runnable onFailure) {
        try {
            String url = BASE_URL + "/v1/players/" + uuid + "/community";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        LOGGER.debug("[PlexMod] Community API status: {}", response.statusCode());
                        if (response.statusCode() == 404) {
                            LOGGER.info("[PlexMod] Player is not in a community.");
                            onFailure.run();
                            return;
                        }
                        if (response.statusCode() != 200) {
                            LOGGER.warn("[PlexMod] Community API unexpected status: {}", response.statusCode());
                            onFailure.run();
                            return;
                        }
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (!json.has("community") || json.get("community").isJsonNull()) {
                                LOGGER.info("[PlexMod] Community API: no community field in response.");
                                onFailure.run();
                                return;
                            }

                            JsonObject community = json.getAsJsonObject("community");
                            String name = community.get("name").getAsString();

                            JsonObject formatting = community.getAsJsonObject("chatFormatting");
                            ChatFormatting prefixColor = parseColor(formatting.get("prefix").getAsString(), ChatFormatting.BLUE);
                            ChatFormatting nameColor   = parseColor(formatting.get("playerName").getAsString(), ChatFormatting.RED);
                            ChatFormatting msgColor    = parseColor(formatting.get("message").getAsString(), ChatFormatting.GREEN);

                            CommunityChannel channel = new CommunityChannel(name);
                            channel.setFormattingStyles(prefixColor, nameColor, msgColor);

                            LOGGER.info("[PlexMod] Community resolved: {} (prefix={}, player={}, msg={})",
                                    name, prefixColor, nameColor, msgColor);
                            onSuccess.accept(channel);
                        } catch (Exception e) {
                            LOGGER.error("[PlexMod] Failed parsing community response", e);
                            onFailure.run();
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("[PlexMod] HTTP error fetching player community", ex);
                        onFailure.run();
                        return null;
                    });
        } catch (Exception e) {
            LOGGER.error("[PlexMod] Failed to build community request for uuid: {}", uuid, e);
            onFailure.run();
        }
    }

    private static ChatFormatting parseColor(String value, ChatFormatting fallback) {
        if (value == null || value.isBlank()) return fallback;
        if (value.startsWith("§") && value.length() == 2) {
            ChatFormatting fmt = ChatFormatting.getByCode(value.charAt(1));
            return fmt != null ? fmt : fallback;
        }
        try {
            return ChatFormatting.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}