package com.example.bulpyeonmabul.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScoreBoardState {
    private final List<PlayerScoreState> players;
    private final int round;

    public ScoreBoardState(List<PlayerScoreState> players, int round) {
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.round = round;
    }

    public ScoreBoardState withPlayers(List<PlayerScoreState> nextPlayers) {
        return new ScoreBoardState(nextPlayers, round);
    }

    public ScoreBoardState withRound(int nextRound) {
        return new ScoreBoardState(players, nextRound);
    }

    public List<PlayerScoreState> getPlayers() {
        return players;
    }

    public int getRound() {
        return round;
    }
}
