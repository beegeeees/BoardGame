package com.example.bulpyeonmabul.score;

public final class ScoreLog {
    private final String playerId;
    private final int delta;
    private final String reason;

    public ScoreLog(String playerId, int delta, String reason) {
        this.playerId = playerId;
        this.delta = delta;
        this.reason = reason;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getDelta() {
        return delta;
    }

    public String getReason() {
        return reason;
    }
}
