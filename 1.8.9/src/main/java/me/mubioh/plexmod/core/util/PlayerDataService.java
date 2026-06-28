package me.mubioh.plexmod.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class PlayerDataService {

    private static final Logger LOGGER        = LogManager.getLogger("PlexMod/PlayerData");
    private static final String BASE_URL      = "https://plexmod.mubiohs.workers.dev";
    private static final long   CACHE_TTL_MS  = 5 * 60 * 1000L;
    private static final int    BATCH_SIZE    = 20;
    private static final long   CYCLE_DELAY_MS = 2_000L;

    private static final Set<String> STAFF_GROUPS = new HashSet<>(Arrays.asList(
            "trainee", "mod", "srmod", "admin", "dev", "owner"
    ));

    private static PlayerDataService instance;
    public static PlayerDataService getInstance() {
        if (instance == null) instance = new PlayerDataService();
        return instance;
    }

    private final Map<String, CachedData>  cache   = new ConcurrentHashMap<>();
    private final Set<String>              pending = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "PlexMod-PlayerData");
                    t.setDaemon(true);
                    return t;
                }
            });

    private volatile boolean isStaff = false;

    private PlayerDataService() {
        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() { processBatch(); }
        }, CYCLE_DELAY_MS, CYCLE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public boolean isLocalPlayerStaff() { return isStaff; }

    public void fetchLocalPlayerRank(final String uuid) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    String body = httpGet(BASE_URL + "/v1/players/" + uuid);
                    isStaff = isStaffFromJson(body);
                } catch (Exception e) {
                    isStaff = false;
                }
            }
        }, "PlexMod-RankFetch");
        t.setDaemon(true);
        t.start();
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "PlexMod/1.1.2");

        int status = conn.getResponseCode();
        if (status == 404) throw new Exception("404 Not Found: " + urlStr);
        if (status == 429) throw new Exception("429 Rate limited");
        if (status != 200) throw new Exception("HTTP " + status + " for " + urlStr);

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

    private static boolean isStaffFromJson(String json) {
        int idx = json.indexOf("\"permissionGroups\"");
        if (idx == -1) return false;
        int start = json.indexOf('[', idx);
        int end   = json.indexOf(']', start);
        if (start == -1 || end == -1) return false;
        String groups = json.substring(start, end).toLowerCase(Locale.ROOT);
        for (String rank : STAFF_GROUPS) {
            if (groups.contains("\"" + rank + "\"")) return true;
        }
        return false;
    }

    public void requestPlayer(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return;
        CachedData existing = cache.get(uuid);
        if (existing != null && !existing.isExpired()) return;
        cache.putIfAbsent(uuid, new CachedData(new PlayerData(0)));
        pending.add(uuid);
    }

    public PlayerData get(String uuid) {
        CachedData c = cache.get(uuid);
        return c == null ? null : c.data;
    }

    public void evict(String uuid) {
        cache.remove(uuid);
        pending.remove(uuid);
    }

    public void shutdown() { executor.shutdownNow(); }

    private void processBatch() {
        if (pending.isEmpty()) return;
        List<String> batch = new ArrayList<>();
        Iterator<String> it = pending.iterator();
        while (it.hasNext() && batch.size() < BATCH_SIZE) {
            batch.add(it.next());
            it.remove();
        }
        for (String uuid : batch) {
            try {
                int level = fetchLevel(uuid);
                cache.put(uuid, new CachedData(new PlayerData(level)));
            } catch (Exception e) {
                LOGGER.debug("[PlexMod] Failed to fetch data for {}: {}", uuid, e.getMessage());
                cache.putIfAbsent(uuid, new CachedData(new PlayerData(0)));
            }
        }
    }

    private int fetchLevel(String uuid) throws Exception {
        String body = httpGet(BASE_URL + "/v1/statistics/" + uuid + "/general");
        long xp = extractLong(body, "\"experience\"");
        return LevelUtil.getLevelProgress(xp).level;
    }

    private static long extractLong(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) throw new NoSuchElementException("Key not found: " + key);
        int colon = json.indexOf(':', idx + key.length());
        if (colon == -1) throw new NoSuchElementException("Colon not found");
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        if (end < json.length() && json.charAt(end) == '-') end++;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        String numStr = json.substring(start, end).trim();
        if (numStr.isEmpty()) throw new NoSuchElementException("No number after key: " + key);
        return Long.parseLong(numStr);
    }

    public static class PlayerData {
        public final int level;
        public PlayerData(int level) { this.level = level; }
    }

    private static class CachedData {
        volatile PlayerData data;
        final long fetchedAt = System.currentTimeMillis();
        CachedData(PlayerData data) { this.data = data; }
        boolean isExpired() { return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS; }
    }
}
