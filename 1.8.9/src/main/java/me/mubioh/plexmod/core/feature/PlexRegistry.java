package me.mubioh.plexmod.core.feature;

import me.mubioh.plexmod.core.config.PlexConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class PlexRegistry {

    private static final Logger LOGGER = LogManager.getLogger("PlexMod/Registry");
    private static final Map<String, PlexFeature> features = new LinkedHashMap<>();

    private PlexRegistry() {}

    public static void register(PlexFeature feature) {
        if (features.containsKey(feature.getId())) {
            LOGGER.warn("Feature '{}' already registered — skipping.", feature.getId());
            return;
        }
        features.put(feature.getId(), feature);

        if (!feature.isToggleable()) {
            feature.onEnable();
            LOGGER.info("Feature '{}' registered (always-on).", feature.getId());
            return;
        }

        boolean enabled = PlexConfig.getInstance().isFeatureEnabled(feature.getId());
        if (enabled) { feature.onEnable();  LOGGER.info("Feature '{}' enabled.",  feature.getId()); }
        else         { feature.onDisable(); LOGGER.info("Feature '{}' disabled.", feature.getId()); }
    }

    public static void setEnabled(String id, boolean enabled) {
        PlexFeature f = features.get(id);
        if (f == null || !f.isToggleable()) return;
        PlexConfig.getInstance().setFeatureEnabled(id, enabled);
        if (enabled) f.onEnable(); else f.onDisable();
    }

    public static boolean isEnabled(String id)    { return PlexConfig.getInstance().isFeatureEnabled(id); }
    public static PlexFeature get(String id)      { return features.get(id); }
    public static Collection<PlexFeature> getAll() { return Collections.unmodifiableCollection(features.values()); }
}
