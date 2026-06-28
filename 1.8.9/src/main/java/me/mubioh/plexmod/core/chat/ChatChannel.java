package me.mubioh.plexmod.core.chat;

public enum ChatChannel {

    ALL("All", null),
    PARTY("Party", "@ "),
    TEAM("Team", "# "),
    COMMUNITY("Community", "!");

    public final String displayName;
    public final String prefix;

    ChatChannel(String displayName, String prefix) {
        this.displayName = displayName;
        this.prefix      = prefix;
    }

    public boolean isAvailable(boolean inParty, boolean inTeamGame) {
        switch (this) {
            case ALL:       return true;
            case PARTY:     return inParty;
            case TEAM:      return inTeamGame;
            case COMMUNITY: return false;
            default:        return false;
        }
    }

    public boolean isAvailable(boolean inParty) {
        return isAvailable(inParty, false);
    }
}
