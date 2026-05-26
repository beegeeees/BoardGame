package com.example.bulpyeonmabul.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScoreApplyResult {
    private final ScoreBoardState state;
    private final List<ScoreLog> logs;

    public ScoreApplyResult(ScoreBoardState state, List<ScoreLog> logs) {
        this.state = state;
        this.logs = Collections.unmodifiableList(new ArrayList<>(logs));
    }

    public ScoreBoardState getState() {
        return state;
    }

    public List<ScoreLog> getLogs() {
        return logs;
    }
}
