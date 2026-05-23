package com.example.boardgame.socket.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotMessageMapper {
    private static final String FIELD_ROOM = "room";
    private static final String FIELD_GAME = "game";
    private static final String FIELD_MINI_GAME = "miniGame";
    private static final String FIELD_MICRO_GAME = "microGame";

    private SnapshotMessageMapper() {
    }

    public static SocketMessage roomUpdated(RoomSnapshot snapshot) {
        return SocketMessage.builder(MessageTypes.ROOM_UPDATED)
                .put(FIELD_ROOM, toJson(snapshot))
                .build();
    }

    public static SocketMessage gameUpdated(GameSnapshot snapshot) {
        return SocketMessage.builder(MessageTypes.GAME_UPDATED)
                .put(FIELD_GAME, toJson(snapshot))
                .build();
    }

    public static SocketMessage miniGameUpdated(MiniGameSnapshot snapshot) {
        return SocketMessage.builder(MessageTypes.MINI_GAME_UPDATED)
                .put(FIELD_MINI_GAME, toJson(snapshot))
                .build();
    }

    public static SocketMessage microGameUpdated(MicroGameSnapshot snapshot) {
        return SocketMessage.builder(MessageTypes.MICRO_GAME_UPDATED)
                .put(FIELD_MICRO_GAME, toJson(snapshot))
                .build();
    }

    public static RoomSnapshot toRoomSnapshot(SocketMessage message) {
        JsonObject room = message.getObject(FIELD_ROOM);
        return new RoomSnapshot(
                string(room, "code"),
                string(room, "hostPlayerId"),
                string(room, "status"),
                players(room.get("players"))
        );
    }

    public static GameSnapshot toGameSnapshot(SocketMessage message) {
        JsonObject game = message.getObject(FIELD_GAME);
        return new GameSnapshot(
                string(game, "roomCode"),
                integer(game, "currentRound", 1),
                integer(game, "finalRound", 5),
                string(game, "currentPlayerId"),
                integer(game, "lastDiceRoll", 0),
                string(game, "turnPhase"),
                strings(game.get("turnOrder")),
                string(game, "lastSystemMessage")
        );
    }

    public static MiniGameSnapshot toMiniGameSnapshot(SocketMessage message) {
        JsonObject json = message.getObject(FIELD_MINI_GAME);
        return new MiniGameSnapshot(
                string(json, "id"),
                string(json, "type"),
                string(json, "status"),
                integer(json, "remainingTime", 0),
                scores(json.get("scores"))
        );
    }

    public static MicroGameSnapshot toMicroGameSnapshot(SocketMessage message) {
        JsonObject json = message.getObject(FIELD_MICRO_GAME);
        return new MicroGameSnapshot(
                string(json, "id"),
                string(json, "type"),
                string(json, "triggerPlayerId"),
                string(json, "status"),
                integer(json, "remainingTime", 0),
                scores(json.get("scores"))
        );
    }

    private static JsonObject toJson(RoomSnapshot snapshot) {
        JsonObject room = new JsonObject();
        room.addProperty("code", snapshot.getCode());
        room.addProperty("hostPlayerId", snapshot.getHostPlayerId());
        room.addProperty("status", snapshot.getStatus());

        JsonArray players = new JsonArray();
        for (PlayerSnapshot player : snapshot.getPlayers()) {
            players.add(toJson(player));
        }
        room.add("players", players);
        return room;
    }

    private static JsonObject toJson(PlayerSnapshot snapshot) {
        JsonObject player = new JsonObject();
        player.addProperty("id", snapshot.getId());
        player.addProperty("nickname", snapshot.getNickname());
        player.addProperty("score", snapshot.getScore());
        player.addProperty("position", snapshot.getPosition());
        player.addProperty("ready", snapshot.isReady());
        player.addProperty("host", snapshot.isHost());
        player.addProperty("inMicroGame", snapshot.isInMicroGame());

        JsonArray itemCards = new JsonArray();
        for (String card : snapshot.getItemCards()) {
            itemCards.add(card);
        }
        player.add("itemCards", itemCards);

        return player;
    }

    private static JsonObject toJson(GameSnapshot snapshot) {
        JsonObject game = new JsonObject();
        game.addProperty("roomCode", snapshot.getRoomCode());
        game.addProperty("currentRound", snapshot.getCurrentRound());
        game.addProperty("finalRound", snapshot.getFinalRound());
        game.addProperty("currentPlayerId", snapshot.getCurrentPlayerId());
        game.addProperty("lastDiceRoll", snapshot.getLastDiceRoll());
        game.addProperty("turnPhase", snapshot.getTurnPhase());

        JsonArray turnOrder = new JsonArray();
        for (String playerId : snapshot.getTurnOrder()) {
            turnOrder.add(playerId);
        }
        game.add("turnOrder", turnOrder);
        game.addProperty("lastSystemMessage", snapshot.getLastSystemMessage());

        return game;
    }

    private static JsonObject toJson(MiniGameSnapshot snapshot) {
        JsonObject json = new JsonObject();
        json.addProperty("id", snapshot.getId());
        json.addProperty("type", snapshot.getType());
        json.addProperty("status", snapshot.getStatus());
        json.addProperty("remainingTime", snapshot.getRemainingTime());
        json.add("scores", toJson(snapshot.getScores()));
        return json;
    }

    private static JsonObject toJson(MicroGameSnapshot snapshot) {
        JsonObject json = new JsonObject();
        json.addProperty("id", snapshot.getId());
        json.addProperty("type", snapshot.getType());
        json.addProperty("triggerPlayerId", snapshot.getTriggerPlayerId());
        json.addProperty("status", snapshot.getStatus());
        json.addProperty("remainingTime", snapshot.getRemainingTime());
        json.add("scores", toJson(snapshot.getScores()));
        return json;
    }

    private static JsonObject toJson(Map<String, Integer> scores) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private static Map<String, Integer> scores(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> scores = new HashMap<>();
        JsonObject object = element.getAsJsonObject();
        for (String key : object.keySet()) {
            scores.put(key, object.get(key).getAsInt());
        }
        return scores;
    }

    private static List<PlayerSnapshot> players(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Collections.emptyList();
        }
        List<PlayerSnapshot> players = new ArrayList<>();
        for (JsonElement playerElement : element.getAsJsonArray()) {
            if (!playerElement.isJsonObject()) {
                continue;
            }
            JsonObject player = playerElement.getAsJsonObject();
            players.add(new PlayerSnapshot(
                    string(player, "id"),
                    string(player, "nickname"),
                    integer(player, "score", 0),
                    integer(player, "position", 0),
                    bool(player, "ready", false),
                    bool(player, "host", false),
                    bool(player, "inMicroGame", false),
                    strings(player.get("itemCards"))
            ));
        }
        return players;
    }

    private static List<String> strings(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Collections.emptyList();
        }
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
