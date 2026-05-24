package com.example.adchaosdemo.socket.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SnapshotMessageMapper {
    private SnapshotMessageMapper() {
    }

    public static RoomSnapshot toRoomSnapshot(SocketMessage message) {
        JsonObject room = message.getObject("room");
        return new RoomSnapshot(
                string(room, "code"),
                string(room, "hostPlayerId"),
                string(room, "status"),
                players(room.get("players"))
        );
    }

    public static GameSnapshot toGameSnapshot(SocketMessage message) {
        JsonObject game = message.getObject("game");
        return new GameSnapshot(
                string(game, "roomCode"),
                integer(game, "currentRound", 1),
                integer(game, "finalRound", 5),
                string(game, "currentPlayerId"),
                integer(game, "lastDiceRoll", 0),
                string(game, "turnPhase"),
                strings(game.get("turnOrder"))
        );
    }

    private static List<PlayerSnapshot> players(JsonElement element) {
        if (element == null || !element.isJsonArray()) return Collections.emptyList();
        List<PlayerSnapshot> result = new ArrayList<>();
        for (JsonElement playerElement : element.getAsJsonArray()) {
            if (!playerElement.isJsonObject()) continue;
            JsonObject player = playerElement.getAsJsonObject();
            result.add(new PlayerSnapshot(
                    string(player, "id"),
                    string(player, "nickname"),
                    integer(player, "score", 0),
                    integer(player, "position", 0),
                    bool(player, "ready", false),
                    bool(player, "host", false)
            ));
        }
        return result;
    }

    private static List<String> strings(JsonElement element) {
        if (element == null || !element.isJsonArray()) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            values.add(value == null || value.isJsonNull() ? "" : value.getAsString());
        }
        return values;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int integer(JsonObject object, String key, int defaultValue) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? defaultValue : value.getAsInt();
    }

    private static boolean bool(JsonObject object, String key, boolean defaultValue) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? defaultValue : value.getAsBoolean();
    }
}

