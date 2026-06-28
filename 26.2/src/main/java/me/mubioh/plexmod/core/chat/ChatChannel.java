package me.mubioh.plexmod.core.chat;

public enum ChatChannel {

    ALL      ("All",       null,  true),
    PARTY    ("Party",     "@ ",  false),
    TEAM     ("Team",      "# ",  false),
    COMMUNITY("Community", "!",  false);

    public final String displayName;
    public final String prefix;
    private final boolean alwaysAvailable;

    ChatChannel(String displayName, String prefix, boolean alwaysAvailable) {
        this.displayName     = displayName;
        this.prefix          = prefix;
        this.alwaysAvailable = alwaysAvailable;
    }

    public boolean isAvailable(boolean inParty, boolean inTeamGame) {
        return isAvailable(inParty, inTeamGame, false);
    }

    public boolean isAvailable(boolean inParty, boolean inTeamGame, boolean inCommunity) {
        return switch (this) {
            case ALL       -> true;
            case PARTY     -> inParty;
            case TEAM      -> inTeamGame;
            case COMMUNITY -> inCommunity;
        };
    }

    public boolean isAvailable(boolean inParty) {
        return isAvailable(inParty, false, false);
    }
}
