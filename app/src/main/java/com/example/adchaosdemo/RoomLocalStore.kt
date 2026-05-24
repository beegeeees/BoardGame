package com.example.adchaosdemo

import kotlin.random.Random

data class RoomMember(
    val id: String,
    val nickname: String,
    val isHost: Boolean,
    val isReady: Boolean,
    val joinedOrder: Long
)

data class RoomState(
    val code: String,
    val isPublic: Boolean,
    val members: List<RoomMember>
)

object RoomLocalStore {
    private const val MAX_MEMBERS = 4
    private var joinOrderSeed = 0L
    private val rooms = linkedMapOf<String, RoomState>()
    private var seeded = false

    private fun nextJoinOrder(): Long {
        joinOrderSeed += 1L
        return joinOrderSeed
    }

    private fun makeCode(): String {
        val n = Random.nextInt(100000, 1000000)
        return n.toString()
    }

    fun seedIfNeeded(myNickname: String) {
        if (seeded) return
        seeded = true
        val hostA = RoomMember("h1", "Host_Alpha", true, true, nextJoinOrder())
        val hostB = RoomMember("h2", "Host_Beta", true, true, nextJoinOrder())
        val p1 = RoomMember("p1", "Player_One", false, false, nextJoinOrder())
        val p2 = RoomMember("p2", "Player_Two", false, true, nextJoinOrder())
        rooms["111111"] = RoomState("111111", true, listOf(hostA, p1))
        rooms["222222"] = RoomState("222222", true, listOf(hostB, p2))
    }

    fun listPublicRooms(search: String): List<DemoRoom> {
        val q = search.trim().lowercase()
        return rooms.values
            .filter { it.isPublic }
            .filter {
                q.isBlank() ||
                    it.code.lowercase().contains(q)
            }
            .map {
                val host = it.members.firstOrNull { m -> m.isHost }?.nickname ?: "-"
                DemoRoom(
                    id = it.code,
                    roomCode = it.code,
                    currentCount = it.members.size,
                    maxCount = MAX_MEMBERS,
                    hostNickname = host
                )
            }
    }

    fun findRoom(code: String): RoomState? = rooms[code.trim().uppercase()]

    fun createRoom(hostNickname: String): RoomState {
        var code = makeCode()
        while (rooms.containsKey(code)) {
            code = makeCode()
        }
        val host = RoomMember(
            id = "m-${Random.nextInt(100000)}",
            nickname = hostNickname,
            isHost = true,
            isReady = false,
            joinedOrder = nextJoinOrder()
        )
        val room = RoomState(code, true, listOf(host))
        rooms[code] = room
        return room
    }

    fun joinRoom(code: String, nickname: String): RoomState? {
        val roomCode = code.trim().uppercase()
        val room = rooms[roomCode] ?: return null
        val exists = room.members.any { it.nickname == nickname }
        if (exists) return room
        if (room.members.size >= MAX_MEMBERS) return room
        val newMember = RoomMember(
            id = "m-${Random.nextInt(100000)}",
            nickname = nickname,
            isHost = false,
            isReady = false,
            joinedOrder = nextJoinOrder()
        )
        val updated = room.copy(members = room.members + newMember)
        rooms[roomCode] = updated
        return updated
    }

    fun updateNickname(code: String, oldNickname: String, newNickname: String): RoomState? {
        val roomCode = code.trim().uppercase()
        val room = rooms[roomCode] ?: return null
        val updatedMembers = room.members.map {
            if (it.nickname == oldNickname) it.copy(nickname = newNickname) else it
        }
        val updated = room.copy(members = updatedMembers)
        rooms[roomCode] = updated
        return updated
    }

    fun toggleReady(code: String, nickname: String): RoomState? {
        val roomCode = code.trim().uppercase()
        val room = rooms[roomCode] ?: return null
        val updatedMembers = room.members.map {
            if (it.nickname == nickname) it.copy(isReady = !it.isReady) else it
        }
        val updated = room.copy(members = updatedMembers)
        rooms[roomCode] = updated
        return updated
    }

    fun leaveRoom(code: String, nickname: String): RoomState? {
        val roomCode = code.trim().uppercase()
        val room = rooms[roomCode] ?: return null
        val leaving = room.members.firstOrNull { it.nickname == nickname } ?: return room
        val remain = room.members.filterNot { it.nickname == nickname }.toMutableList()
        if (remain.isEmpty()) {
            rooms.remove(roomCode)
            return null
        }
        if (leaving.isHost) {
            val nextHost = remain.minByOrNull { it.joinedOrder }
            if (nextHost != null) {
                val reassigned = remain.map { member ->
                    if (member.id == nextHost.id) member.copy(isHost = true) else member.copy(isHost = false)
                }
                val updated = room.copy(members = reassigned)
                rooms[roomCode] = updated
                return updated
            }
        }
        val updated = room.copy(members = remain)
        rooms[roomCode] = updated
        return updated
    }

    fun canStart(code: String): Boolean {
        val room = rooms[code.trim().uppercase()] ?: return false
        if (room.members.size !in 2..MAX_MEMBERS) return false
        return room.members.all { it.isReady }
    }
}
