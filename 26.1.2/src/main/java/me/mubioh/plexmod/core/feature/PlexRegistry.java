package me.mubioh.plexmod.core.feature;

import me.mubioh.plexmod.core.config.PlexConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlexRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/Registry");

    private static final Map<String, PlexFeature> features = new LinkedHashMap<>();

    private PlexRegistry() {}

    public static void register(PlexFeature feature) {
        if (features.containsKey(feature.getId())) {
            LOGGER.warn("Feature '{}' is already registered — skipping.", feature.getId());
            return;
        }

        features.put(feature.getId(), feature);

        if (!feature.isToggleable()) {
            feature.onEnable();
            LOGGER.info("Feature '{}' registered (always-on).", feature.getId());
            return;
        }

        boolean enabled = PlexConfig.getInstance().isFeatureEnabled(feature.getId());
        if (enabled) {
            feature.onEnable();
            LOGGER.info("Feature '{}' registered and enabled.", feature.getId());
        } else {
            feature.onDisable();
            LOGGER.info("Feature '{}' registered and disabled.", feature.getId());
        }
    }

    public static void setEnabled(String featureId, boolean enabled) {
        PlexFeature feature = features.get(featureId);
        if (feature == null) {
            LOGGER.warn("Tried to toggle unknown feature '{}'.", featureId);
            return;
        }
        if (!feature.isToggleable()) {
            LOGGER.warn("Feature '{}' is not toggleable.", featureId);
            return;
        }

        PlexConfig.getInstance().setFeatureEnabled(featureId, enabled);

        if (enabled) {
            feature.onEnable();
        } else {
            feature.onDisable();
        }
    }

    public static boolean isEnabled(String featureId) {
        return PlexConfig.getInstance().isFeatureEnabled(featureId);
    }

    public static PlexFeature get(String featureId) {
        return features.get(featureId);
    }

    public static Collection<PlexFeature> getAll() {
        return Collections.unmodifiableCollection(features.values());
    }
}
