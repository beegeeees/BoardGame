package com.example.boardgame.socket.protocol;

public class PlayerSnapshot {
    private final String id;
    private final String nickname;
    private final int score;
    private final int position;
    private final boolean ready;
    private final boolean host;

    public PlayerSnapshot(String id, String nickname, int score, int position, boolean ready, boolean host) {
        this.id = id == null ? "" : id;
        this.nickname = nickname == null ? "" : nickname;
        this.score = score;
        this.position = position;
        this.ready = ready;
        this.host = host;
    }

    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }

    public int getPosition() {
        return position;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isHost() {
        return host;
    }
}
