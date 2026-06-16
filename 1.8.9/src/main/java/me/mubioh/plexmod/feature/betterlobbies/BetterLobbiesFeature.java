package me.mubioh.plexmod.feature.betterlobbies;
import me.mubioh.plexmod.core.feature.PlexFeature;
public class BetterLobbiesFeature implements PlexFeature {
    private static BetterLobbiesFeature instance;
    public static BetterLobbiesFeature getInstance() { return instance; }
    @Override public String getId()          { return "better_lobbies"; }
    @Override public String getDisplayName() { return "Cleaner Lobbies"; }
    @Override public String getTooltip()     { return "Hides boss bars and mutes annoying sounds in the lobby."; }
    @Override public void onEnable()  { instance = this; }
    @Override public void onDisable() { instance = null; }
}
