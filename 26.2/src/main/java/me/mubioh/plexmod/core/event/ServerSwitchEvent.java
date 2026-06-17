package me.mubioh.plexmod.core.event;

public class ServerSwitchEvent extends Event {

    private final String serverName;
    private final boolean isLobby;

    public ServerSwitchEvent(String serverName) {
        this.serverName = serverName;
        this.isLobby = serverName != null &&
                (serverName.toLowerCase().contains("lobby") ||
                        serverName.toLowerCase().contains("hub"));
    }

    public String getServerName() {
        return serverName;
    }

    public boolean isLobby() {
        return isLobby;
    }

    public String getGameType() {
        if (serverName == null) return null;
        int dash = serverName.lastIndexOf('-');
        if (dash > 0) return serverName.substring(0, dash);
        return serverName;
    }
}
