package server

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class GameVerticle : CoroutineVerticle() {
    override suspend fun start() {
        super.start()

        launch {
            uuid()
        }
        launch {
            nickname()
        }
        launch {
            online()
        }
        launch {
            room()
        }
        launch {
            login()
        }
    }

    override suspend fun stop() {


        super.stop()
    }

    private suspend fun uuid() {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.uuid")
        val channel = consumer.toChannel(vertx)
        val uuidSet = hashSetOf<String>()

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action", "")
            when (action) {
                "new" -> {
                    do {
                        val newUUID = UUID.randomUUID().toString()
                        val alreadyExists = newUUID in uuidSet
                        if (!alreadyExists) {
                            uuidSet.add(newUUID)
                            msg.reply(jsonObjectOf(
                                    "uuid" to newUUID
                            ))
                        }
                    } while (alreadyExists)
                }
            }
        }
    }

    private suspend fun nickname() {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.nickname")
        val channel = consumer.toChannel(vertx)
        val nicknameMap = hashMapOf<String, String>()

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            val uuid = request.getString("uuid")
            when (action) {
                "new" -> {
                    val name = "player ${nicknameMap.size}"
                    nicknameMap[uuid] = name
                    msg.reply(jsonObjectOf(
                            "name" to name
                    ))
                }
                "get" -> {
                    msg.reply(jsonObjectOf(
                            "name" to nicknameMap[uuid]
                    ))
                }
                "set" -> {
                    val name = request.getString("name")
                    nicknameMap[uuid] = name
                }
            }
        }
    }

    private suspend fun online() {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.online")
        val channel = consumer.toChannel(vertx)
        val lastRequestTimeMap = hashMapOf<String, Long>()

        val timeout = 10000L

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            val uuid = request.getString("uuid")
            when (action) {
                "set" -> {
                    lastRequestTimeMap[uuid] = System.currentTimeMillis()
                }
                "get" -> {
                    val reqTime = lastRequestTimeMap[uuid] ?: 0L
                    msg.reply(jsonObjectOf(
                            "online" to ((System.currentTimeMillis() - reqTime) < timeout)
                    ))
                }
            }
        }
    }

    private suspend fun room() {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.room")
        val channel = consumer.toChannel(vertx)

        val playerRoomMap = hashMapOf<String, String>()
        val roomSet = hashSetOf<String>()

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            val uuid = request.getString("uuid", "")

            when (action) {
                "enter" -> {
                    if (uuid in playerRoomMap) {
                        vertx.eventBus().send("game.server.match.${playerRoomMap[uuid]}",
                                jsonObjectOf("action" to "leave", "uuid" to uuid))
                    }
                    val roomId = request.getString("id")
                    playerRoomMap[uuid] = roomId
                    // create match
                    if (roomId !in roomSet) {
                        roomSet.add(roomId)
                        launch {
                            match(roomId)
                        }
                    }
                    launch {
                        val color = vertx.eventBus().requestAwait<JsonObject>("game.server.match.$roomId",
                                jsonObjectOf("action" to "enter", "uuid" to uuid))
                                .body().getString("color")
                        msg.reply(jsonObjectOf("color" to color))
                    }
                }
                "leave" -> {
                    if (uuid in playerRoomMap) {
                        vertx.eventBus().send("game.server.match.${playerRoomMap[uuid]}",
                                jsonObjectOf("action" to "leave", "uuid" to uuid))
                        playerRoomMap.remove(uuid)
                    }
                }
                // msg from match when both players leave
                "delete" -> {
                    val roomId = request.getString("id")
                    roomSet.remove(roomId)
                    msg.reply(jsonObjectOf())
                }
            }
        }
    }

    data class Player(var uuid: String = "", var ready: Boolean = false, val color: String, val opponent: Int)

    private suspend fun match(roomId: String) {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.match.$roomId")
        val channel = consumer.toChannel(vertx)

        val timeout = 30

        val players = arrayListOf(
                Player(color = "black", opponent = 2),
                Player(color = "white", opponent = 1)
        )

        val bothReady = Mutex(true)

        val end = AtomicBoolean(false)
        var winner: Int
        var board: IntArray
        var boardAvailable = 100

        fun IntArray.get(x: Int, y: Int): Int {
            return this[x * 10 + y]
        }

        fun IntArray.set(x: Int, y: Int, value: Int) {
            this[x * 10 + y] = value
        }

        fun IntArray.win(x: Int, y: Int): Boolean {
            val player = this.get(x, y)
            val directionList = arrayListOf<Pair<Int, Int>>(
                    0 to 1, 1 to 0, 1 to 1, 1 to -1
            )
            for ((dx, dy) in directionList) {
                var maxContinuation = 1
                for (sign in arrayListOf(-1, 1)) {
                    for (dd in 1..4) {
                        if (this.get(x + dx * dd * sign, y + dy * dd * sign) == player) {
                            maxContinuation++
                        } else {
                            break
                        }
                    }
                }
                if (maxContinuation > 4) {
                    return true
                }
            }
            return false
        }

        launch {
            // start game
            while (true) {
                bothReady.lock()
                players[0].ready = false
                players[1].ready = false
                end.set(false)
                winner = 0
                board = IntArray(100)

                var currentPut = 1
                while (true) {
                    winner = players[currentPut - 1].opponent
                    val put1 = withTimeoutOrNull(timeout * 1000L) {
                        vertx.eventBus().send("$clentPrefix.${players[players[currentPut - 1].opponent - 1].uuid}.b",
                                jsonObjectOf("action" to "opponent to put", "timeout" to timeout))
                        vertx.eventBus().requestAwait<JsonObject>("$clentPrefix.${players[currentPut - 1].uuid}.b",
                                jsonObjectOf("action" to "to put", "timeout" to timeout)).body()
                    } ?: break

                    val x = put1.getInteger("x")
                    val y = put1.getInteger("y")

                    if (board.get(x, y) != 0) {
                        break
                    }
                    boardAvailable--
                    board.set(x, y, currentPut)

                    vertx.eventBus().send("$clentPrefix.${players[players[currentPut - 1].opponent - 1].uuid}.b",
                            jsonObjectOf("action" to "put", "x" to x, "y" to y))

                    if (board.win(x, y)) {
                        winner = currentPut
                        break
                    }

                    if (boardAvailable == 0) {
                        winner = 0
                        break
                    }

                    currentPut = if (currentPut == 1) 2 else 1
                }
                end.set(true)
                val winnerName = if (winner > 0) {
                    players[winner - 1].color
                } else {
                    "none"
                }
                for (player in players) {
                    vertx.eventBus().send("$clentPrefix.${player.uuid}.b",
                            jsonObjectOf("action" to "end", "winner" to winnerName))
                }
            }
        }

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            val uuid = request.getString("uuid")
            when (action) {
                "enter" -> {
                    var success = false
                    for (player in players) {
                        if (player.uuid == "") {
                            success = true
                            player.uuid = uuid
                            player.ready = true
                            msg.reply(jsonObjectOf("color" to player.color))
                            break
                        }
                    }
                    if (!success) {
                        msg.reply(jsonObjectOf("color" to "none"))
                    }
                    if (players[0].ready && players[1].ready) {
                        // send opponent nickname
                        val playerName1 = vertx.eventBus().requestAwait<JsonObject>("game.server.nickname",
                                jsonObjectOf("action" to "get", "uuid" to players[0].uuid))
                                .body().getString("name")
                        val playerName2 = vertx.eventBus().requestAwait<JsonObject>("game.server.nickname",
                                jsonObjectOf("action" to "get", "uuid" to players[1].uuid))
                                .body().getString("name")
                        vertx.eventBus().send("game.player.${players[0].uuid}.b",
                                jsonObjectOf("action" to "opponent nickname", "name" to playerName2))
                        vertx.eventBus().send("game.player.${players[1].uuid}.b",
                                jsonObjectOf("action" to "opponent nickname", "name" to playerName1))
                        bothReady.unlock()
                    }
                }
                "leave" -> {
                    for (player in players) {
                        if (player.uuid == uuid) {
                            player.uuid = ""
                            player.ready = false
                            winner = player.opponent
                            end.set(true)
                            break
                        }
                    }
                }
                "reset" -> {
                    if (end.get()) {
                        for (player in players) {
                            if (player.uuid == uuid) {
                                player.ready = true
                                break
                            }
                        }
                        if (players[0].ready && players[1].ready) {
                            bothReady.unlock()
                        }
                    }
                }
                "update name" -> {
                    val name = request.getString("name")
                    val opponentUUID = if (players[0].uuid == uuid) players[1].uuid else players[0].uuid
                    if (opponentUUID != "") {
                        vertx.eventBus().send("game.player.$opponentUUID.b",
                                jsonObjectOf("action" to "opponent nickname", "name" to name))
                    }
                }
            }
        }
    }

    private val noAction = "no action field in json"

    private suspend fun login() {
        val consumer = vertx.eventBus().consumer<JsonObject>("$clentPrefix.s")
        val channel = consumer.toChannel(vertx)


        for (msg in channel) {
            val request = msg.body()
            println(request.toString())
            val action = request.getString("action", noAction)
            when (action) {
                "new uuid" -> {
                    val uuid = vertx.eventBus().requestAwait<JsonObject>("game.server.uuid",
                            jsonObjectOf("action" to "new")).body().getString("uuid")
                    val name = vertx.eventBus().requestAwait<JsonObject>("game.server.nickname",
                            jsonObjectOf("action" to "new", "uuid" to uuid)).body().getString("name")
                    launch {
                        player(uuid)
                    }
                    val result = jsonObjectOf(
                            "action" to "new uuid",
                            "uuid" to uuid,
                            "nickname" to name
                    )
                    println(result.toString())
                    msg.reply(result)
                }
                else -> {
                    msg.reply(jsonObjectOf(
                            "action" to "error",
                            "msg" to "wrong action: $action"
                    ))
                }
            }
        }
    }

    private suspend fun player(uuid: String) {
        val consumer = vertx.eventBus().consumer<JsonObject>("$clentPrefix.$uuid.s")
        val channel = consumer.toChannel(vertx)
        var playerRoom = ""

        for (msg in channel) {
            vertx.eventBus().send("game.server.online",
                    jsonObjectOf("action" to "set", "uuid" to uuid))

            val request = msg.body()
            val action = request.getString("action", noAction)
            when (action) {
                "update nickname" -> {
                    val name = request.getString("name", "default name")
                    vertx.eventBus().send("game.server.nickname",
                            jsonObjectOf("action" to "set", "uuid" to uuid, "name" to name))
                    if (playerRoom != "") {
                        vertx.eventBus().send("game.server.match.$playerRoom",
                                jsonObjectOf("action" to "update name", "uuid" to uuid, "name" to name))
                    }
                }
                "heart beat" -> {
                    // no-op
                }
                "enter room" -> {
                    val roomId = request.getString("id", "default room")
                    playerRoom = roomId
                    val color = vertx.eventBus().requestAwait<JsonObject>("game.server.room",
                            jsonObjectOf("action" to "enter", "uuid" to uuid, "id" to roomId))
                            .body().getString("color")
                    msg.reply(jsonObjectOf(
                            "action" to "enter room",
                            "color" to color
                    ))
                }
                "leave room" -> {
                    vertx.eventBus().send("game.server.room",
                            jsonObjectOf("action" to "leave", "uuid" to uuid))
                    playerRoom = ""
                }
                else -> {
                    msg.reply(jsonObjectOf(
                            "action" to "error",
                            "msg" to "wrong action: $action"
                    ))
                }
            }
        }
    }
}