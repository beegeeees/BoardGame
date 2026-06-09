package com.example.boardgame.socket.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomSnapshot {
    private final String code;
    private final String hostPlayerId;
    private final String status;
    private final long revision;
    private final List<PlayerSnapshot> players;

    public RoomSnapshot(String code, String hostPlayerId, String status, List<PlayerSnapshot> players) {
        this(code, hostPlayerId, status, 0L, players);
    }

    public RoomSnapshot(String code, String hostPlayerId, String status, long revision, List<PlayerSnapshot> players) {
        this.code = code == null ? "" : code;
        this.hostPlayerId = hostPlayerId == null ? "" : hostPlayerId;
        this.status = status == null ? "" : status;
        this.revision = Math.max(0L, revision);
        this.players = new ArrayList<>(players == null ? Collections.emptyList() : players);
    }

    public String getCode() {
        return code;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public String getStatus() {
        return status;
    }

    public long getRevision() {
        return revision;
    }

    public List<PlayerSnapshot> getPlayers() {
        return Collections.unmodifiableList(players);
    }
}
