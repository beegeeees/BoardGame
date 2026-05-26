package com.example.bulpyeonmabul.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScoreSnapshot {
    private final int round;
    private final List<ScoreViewRow> ranking;
    private final List<ScoreLog> logs;

    public ScoreSnapshot(int round, List<ScoreViewRow> ranking, List<ScoreLog> logs) {
        this.round = round;
        this.ranking = Collections.unmodifiableList(new ArrayList<>(ranking));
        this.logs = Collections.unmodifiableList(new ArrayList<>(logs));
    }

    public int getRound() {
        return round;
    }

    public List<ScoreViewRow> getRanking() {
        return ranking;
    }

    public List<ScoreLog> getLogs() {
        return logs;
    }
}
