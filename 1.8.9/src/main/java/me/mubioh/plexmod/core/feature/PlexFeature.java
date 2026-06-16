package me.mubioh.plexmod.core.feature;

public interface PlexFeature {
    String  getId();
    String  getDisplayName();
    String  getTooltip();
    void    onEnable();
    void    onDisable();
    default boolean isToggleable() { return true; }
}
