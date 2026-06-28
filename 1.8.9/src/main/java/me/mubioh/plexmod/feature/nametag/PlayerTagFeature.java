package me.mubioh.plexmod.feature.nametag;

import me.mubioh.plexmod.core.feature.PlexFeature;

public class PlayerTagFeature implements PlexFeature {
    @Override public String getId()          { return "player_tag"; }
    @Override public String getDisplayName() { return "Player Tag"; }
    @Override public String getTooltip()     { return "Shows player levels above their nametag."; }
    @Override public void onEnable()         {}
    @Override public void onDisable()        {}
}