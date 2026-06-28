package me.mubioh.plexmod.core.util;

public final class LevelUtil {

    private static final long[] THRESHOLDS = {
        0, 500, 1500, 3000, 5000, 7500, 10500, 14000, 18000, 22500, 27500,
        33500, 40500, 48500, 57500, 67500, 78500, 90500, 103500, 117500, 132500,
        149500, 168500, 189500, 212500, 237500, 264500, 293500, 324500, 357500, 392500,
        429500, 468500, 509500, 552500, 597500, 644500, 693500, 744500, 797500, 852500,
        910500, 971500, 1035500, 1102500, 1172500, 1245500, 1321500, 1400500, 1482500, 1567500,
        1655500, 1746500, 1840500, 1937500, 2037500, 2140500, 2246500, 2355500, 2467500, 2582500,
        2701500, 2824500, 2951500, 3082500, 3217500, 3356500, 3499500, 3646500, 3797500, 3952500,
        4111500, 4274500, 4441500, 4612500, 4787500, 4966500, 5149500, 5336500, 5527500, 5722500,
        5922500, 6127500, 6337500, 6552500, 6772500, 6997500, 7227500, 7462500, 7702500, 7947500,
        8197500, 8452500, 8712500, 8977500, 9247500, 9522500, 9802500, 10087500, 10377500, 10672500
    };

    private LevelUtil() {}

    public static int getLevel(long xp) {
        if (xp < 0) return 0;
        int level = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (xp >= THRESHOLDS[i]) level = i;
            else break;
        }
        return level;
    }

    public static LevelProgress getLevelProgress(long xp) {
        if (xp < 0) xp = 0;
        int level = getLevel(xp);

        if (level >= 100) {
            return new LevelProgress(100, xp, THRESHOLDS[100], null, 100);
        }

        long currentThreshold = THRESHOLDS[level];
        long nextThreshold    = THRESHOLDS[level + 1];
        int  pct              = (int) (((xp - currentThreshold) * 100) / (nextThreshold - currentThreshold));

        return new LevelProgress(level, xp, currentThreshold, nextThreshold, pct);
    }

    public record LevelProgress(int level, long xp, long currentThreshold, Long nextThreshold, int pct) {}
}
