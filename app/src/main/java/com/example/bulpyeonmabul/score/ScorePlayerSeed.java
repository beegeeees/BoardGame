package com.example.bulpyeonmabul.score;

public final class ScorePlayerSeed {
    private final String playerId;
    private final String nickname;

    public ScorePlayerSeed(String playerId, String nickname) {
        this.playerId = playerId;
        this.nickname = nickname;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }
}
