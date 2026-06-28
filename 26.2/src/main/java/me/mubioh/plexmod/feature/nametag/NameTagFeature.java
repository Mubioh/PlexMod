// NameTagFeature.java - unchanged from original, just own nametag visibility
package me.mubioh.plexmod.feature.nametag;

import me.mubioh.plexmod.core.feature.PlexFeature;

public class NameTagFeature implements PlexFeature {

    @Override public String getId()          { return "nametag"; }
    @Override public String getDisplayName() { return "Nametag"; }
    @Override public String getTooltip()     { return "See your own nametag in third-person."; }
    @Override public void onEnable()         {}
    @Override public void onDisable()        {}
}