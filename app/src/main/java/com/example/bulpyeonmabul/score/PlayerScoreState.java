package com.example.bulpyeonmabul.score;

public final class PlayerScoreState {
    private final String playerId;
    private final String nickname;
    private final int score;
    private final int miniGameWinCount;

    public PlayerScoreState(String playerId, String nickname) {
        this(playerId, nickname, 0, 0);
    }

    public PlayerScoreState(String playerId, String nickname, int score, int miniGameWinCount) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.score = score;
        this.miniGameWinCount = miniGameWinCount;
    }

    public PlayerScoreState withScore(int nextScore) {
        return new PlayerScoreState(playerId, nickname, nextScore, miniGameWinCount);
    }

    public PlayerScoreState withScoreAndWinCount(int nextScore, int nextMiniGameWinCount) {
        return new PlayerScoreState(playerId, nickname, nextScore, nextMiniGameWinCount);
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
