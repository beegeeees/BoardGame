package com.example.bulpyeonmabul.score;

public final class ScoreViewRow {
    private final String playerId;
    private final String nickname;
    private final int score;
    private final int miniGameWinCount;

    public ScoreViewRow(String playerId, String nickname, int score, int miniGameWinCount) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.score = score;
        this.miniGameWinCount = miniGameWinCount;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }

    public int getMiniGameWinCount() {
        return miniGameWinCount;
    }
}
