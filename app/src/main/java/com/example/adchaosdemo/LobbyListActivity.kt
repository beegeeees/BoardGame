package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adchaosdemo.socket.protocol.ConnectionState

class LobbyListActivity : AppCompatActivity() {
    private enum class PendingAction {
        NONE,
        CREATE_ROOM,
        JOIN_ROOM
    }

    private lateinit var roomAdapter: RoomListAdapter
    private val rooms = mutableListOf<DemoRoom>()
    private val serverRoomCodes = linkedSetOf<String>()
    private var myNickname: String = ""
    private var pendingRoomCode: String? = null
    private var pendingAction: PendingAction = PendingAction.NONE
    private var debugMode: Boolean = false
    private val lobbyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val leftCode = result.data?.getStringExtra(LobbyActivity.EXTRA_LEFT_ROOM_CODE).orEmpty()
        if (!debugMode && leftCode.isNotBlank()) {
            if (serverRoomCodes.remove(leftCode)) {
                saveServerRoomCodes()
            }
        }
        reloadRooms()
    }

    private val gatewayListener = object : ServerRoomGateway.Listener {
        override fun onConnectionStateChanged(state: ConnectionState) {
            if (debugMode) return
            if (state == ConnectionState.DISCONNECTED) {
                Toast.makeText(this@LobbyListActivity, "서버 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServerError(message: String) {
            if (debugMode) return
            if (pendingAction == PendingAction.JOIN_ROOM) {
                val badCode = pendingRoomCode
                if (!badCode.isNullOrBlank() && serverRoomCodes.remove(badCode)) {
                    saveServerRoomCodes()
                    reloadRooms()
                }
            }
            pendingAction = PendingAction.NONE
            pendingRoomCode = null
            Toast.makeText(this@LobbyListActivity, message, Toast.LENGTH_SHORT).show()
        }

        override fun onRequestOk(type: String, roomCode: String, playerId: String) {
            if (debugMode) return
            if (roomCode.isBlank()) return

            when (pendingAction) {
                PendingAction.NONE -> return
                PendingAction.JOIN_ROOM -> {
                    val requested = pendingRoomCode ?: return
                    if (!requested.equals(roomCode, ignoreCase = true)) return
                    pendingAction = PendingAction.NONE
                    pendingRoomCode = null
                    addServerRoomCode(roomCode)
                    openRoom(buildServerRoom(roomCode))
                }
                PendingAction.CREATE_ROOM -> {
                    pendingAction = PendingAction.NONE
                    pendingRoomCode = null
                    addServerRoomCode(roomCode)
                    openRoom(buildServerRoom(roomCode))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby_list)

        val recyclerView = findViewById<RecyclerView>(R.id.roomRecyclerView)
        val createRoomButton = findViewById<Button>(R.id.createRoomButton)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val backTitleButton = findViewById<Button>(R.id.backToTitleButton)
        val roomCodeInput = findViewById<EditText>(R.id.roomCodeInput)
        val joinByCodeButton = findViewById<Button>(R.id.joinByCodeButton)

        myNickname = SessionPrefs.getNickname(this).ifBlank { "PlayerMe" }
        debugMode = SessionPrefs.isDebugMode(this)
        loadServerRoomCodes()

        roomAdapter = RoomListAdapter(myNickname) { room ->
            if (debugMode) {
                openRoom(room)
            } else {
                pendingAction = PendingAction.JOIN_ROOM
                pendingRoomCode = room.roomCode
                ServerRoomGateway.joinRoom(this, room.roomCode, myNickname)
                Toast.makeText(this, "입장 요청 중...", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = roomAdapter

        RoomLocalStore.seedIfNeeded(myNickname)
        reloadRooms()

        createRoomButton.setOnClickListener {
            if (debugMode) {
                val room = RoomLocalStore.createRoom(myNickname).toDemoRoom()
                reloadRooms()
                openRoom(room)
            } else {
                pendingAction = PendingAction.CREATE_ROOM
                pendingRoomCode = null
                ServerRoomGateway.createRoom(this, myNickname)
                Toast.makeText(this, "방 생성 요청 중...", Toast.LENGTH_SHORT).show()
            }
        }

        refreshButton.setOnClickListener {
            reloadRooms()
        }

        joinByCodeButton.setOnClickListener {
            val code = roomCodeInput.text.toString().trim()
            if (code.isBlank()) {
                roomCodeInput.error = getString(R.string.lobby_list_code_required)
                return@setOnClickListener
            }
            if (debugMode) {
                val room = RoomLocalStore.findRoom(code)
                if (room == null) {
                    roomCodeInput.error = getString(R.string.lobby_list_code_not_found)
                    return@setOnClickListener
                }
                val joined = RoomLocalStore.joinRoom(code, myNickname) ?: room
                openRoom(joined.toDemoRoom())
            } else {
                pendingAction = PendingAction.JOIN_ROOM
                pendingRoomCode = code
                ServerRoomGateway.joinRoom(this, code, myNickname)
                Toast.makeText(this, "입장 요청 중...", Toast.LENGTH_SHORT).show()
            }
        }

        backTitleButton.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        ServerRoomGateway.addListener(gatewayListener)
    }

    override fun onStop() {
        ServerRoomGateway.removeListener(gatewayListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        debugMode = SessionPrefs.isDebugMode(this)
        loadServerRoomCodes()
        reloadRooms()
    }

    private fun reloadRooms() {
        rooms.clear()
        if (debugMode) {
            rooms.addAll(RoomLocalStore.listPublicRooms(""))
        } else {
            rooms.addAll(serverRoomCodes.map { buildServerRoom(it) })
        }
        roomAdapter.submit(rooms)
    }

    private fun buildServerRoom(code: String): DemoRoom {
        return DemoRoom(
            id = code,
            roomCode = code,
            currentCount = 1,
            maxCount = 4,
            hostNickname = "-"
        )
    }

    private fun openRoom(room: DemoRoom) {
        val myName = SessionPrefs.getNickname(this).ifBlank { "PlayerMe" }
        val intent = Intent(this, LobbyActivity::class.java).apply {
            putExtra(LobbyActivity.EXTRA_MY_NICKNAME, myName)
            putExtra(LobbyActivity.EXTRA_ROOM_CODE, room.roomCode)
        }
        lobbyLauncher.launch(intent)
    }

    private fun RoomState.toDemoRoom(): DemoRoom {
        val host = members.firstOrNull { it.isHost }?.nickname ?: "-"
        return DemoRoom(
            id = code,
            roomCode = code,
            currentCount = members.size,
            maxCount = 4,
            hostNickname = host
        )
    }

    private fun loadServerRoomCodes() {
        val prefs = getSharedPreferences("lobby_list_server_rooms", MODE_PRIVATE)
        val csv = prefs.getString("codes", "").orEmpty()
        serverRoomCodes.clear()
        csv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { serverRoomCodes.add(it) }
    }

    private fun saveServerRoomCodes() {
        val prefs = getSharedPreferences("lobby_list_server_rooms", MODE_PRIVATE)
        prefs.edit().putString("codes", serverRoomCodes.joinToString(",")).apply()
    }

    private fun addServerRoomCode(code: String) {
        if (serverRoomCodes.add(code)) {
            saveServerRoomCodes()
            reloadRooms()
        }
    }
}
