package server

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.deliveryOptionsOf
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.eventbus.unregisterAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.*
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
                            "time_passed" to ((System.currentTimeMillis() - reqTime) / 1000L).toInt()
                    ))
                }
            }
        }
    }

    private suspend fun room() {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.room")
        val channel = consumer.toChannel(vertx)

        val playerRoomMap = hashMapOf<String, String>()
        val roomUsage = hashMapOf<String, Int>()
        val roomSet = hashSetOf<String>()

        fun leaveRoom(uuid: String) {
            if (uuid in playerRoomMap) {
                vertx.eventBus().send("game.server.match.${playerRoomMap[uuid]}",
                        jsonObjectOf("action" to "leave", "uuid" to uuid))
                val roomId = playerRoomMap[uuid]!!
                roomUsage[roomId] = roomUsage[roomId]!! - 1
                if (roomUsage[roomId] == 0) {
                    roomUsage.remove(roomId)
                    roomSet.remove(roomId)
                }
                playerRoomMap.remove(uuid)
            }
        }


        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            val uuid = request.getString("uuid", "")

            when (action) {
                "enter" -> {
                    if (uuid in playerRoomMap) {
                        leaveRoom(uuid)
                    }
                    val roomId = request.getString("id")
                    playerRoomMap[uuid] = roomId
                    if (roomId in roomUsage) {
                        roomUsage[roomId] = roomUsage[roomId]!! + 1
                    } else {
                        roomUsage[roomId] = 1
                    }
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
                    leaveRoom(uuid)
                }
                "reset" -> {
                    if (uuid in playerRoomMap) {
                        vertx.eventBus().send("game.server.match.${playerRoomMap[uuid]}",
                                jsonObjectOf("action" to "reset", "uuid" to uuid))
                    }
                }
                // msg from match when both players leave
//                "delete" -> {
//                    val roomId = request.getString("id")
//                    roomSet.remove(roomId)
//                    roomUsage.remove(roomId)
//                    msg.reply(jsonObjectOf())
//                }
            }
        }
    }


    data class Player(var uuid: String = "", var ready: Boolean = false, val color: String, val opponent: Int)

    private suspend fun match(roomId: String) {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.match.$roomId")
        val channel = consumer.toChannel(vertx)

        val timeout = 30
        var matchRoutine: Job? = null

        val players = arrayListOf(
                Player(color = "black", opponent = 2),
                Player(color = "white", opponent = 1)
        )

        val end = AtomicBoolean(false)

        fun IntArray.get(x: Int, y: Int): Int {
            val index = x * 10 + y
            if (index !in 0..99) {
                return -1;
            } else {
                return this[index]
            }
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

        suspend fun oneMatch() {
            end.set(false)
            players[0].ready = false
            players[1].ready = false
            var winner = 0
            val board = IntArray(100)
            var boardAvailable = 100

            try {
                var currentPut = 1
                matching@
                while (true) {
                    winner = players[currentPut - 1].opponent
                    vertx.eventBus().send("$clentPrefix.${players[players[currentPut - 1].opponent - 1].uuid}.b",
                            jsonObjectOf("action" to "opponent to put", "timeout" to timeout))
                    val put1 = try {
                        vertx.eventBus().requestAwait<JsonObject>(
                                "$clentPrefix.${players[currentPut - 1].uuid}.b",
                                jsonObjectOf("action" to "to put", "timeout" to timeout),
                                deliveryOptionsOf(sendTimeout = timeout * 1000L)
                        ).body()
                    } catch (e: ReplyException) {
                        when (e.failureType()) {
                            ReplyFailure.TIMEOUT -> break@matching
                            ReplyFailure.NO_HANDLERS -> break@matching
                            else -> {
                                println("error when request client for 'to put'")
                                e.printStackTrace()
                                break@matching
                            }
                        }
                    }

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
            } finally {
                end.set(true)

                if (players[0].uuid == "" && players[1].uuid == "") {
                    winner = 0
                } else if (players[0].uuid == "") {
                    winner = 2
                } else if (players[1].uuid == "") {
                    winner = 1
                }

                val winnerName = if (winner > 0) {
                    players[winner - 1].color
                } else {
                    "none"
                }
                withContext(NonCancellable) {
                    for (player in players) {
                        if (player.uuid != "") {
                            vertx.eventBus().send("$clentPrefix.${player.uuid}.b",
                                    jsonObjectOf("action" to "end", "winner" to winnerName))
                        }
                    }
                }

                matchRoutine = null
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
                    if (players[0].uuid != "" && players[1].uuid != "") {
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
                    }
                }
                "leave" -> {
                    for (player in players) {
                        if (player.uuid == uuid) {
                            player.uuid = ""
                            player.ready = false
                            end.set(true)
                            val opponentUUID = players[player.opponent - 1].uuid
                            if (opponentUUID != "") {
                                vertx.eventBus().send("game.player.$opponentUUID.b",
                                        jsonObjectOf("action" to "opponent nickname", "name" to null))
                            }
                            matchRoutine?.cancel()
                            break
                        }
                    }
                    if (players[0].uuid == "" && players[1].uuid == "") {
                        consumer.unregisterAwait()
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
            if (action == "enter" || action == "reset") {
                if (players[0].ready && players[1].ready && matchRoutine == null) {
                    matchRoutine = launch {
                        oneMatch()
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

        println("player: started player $uuid")

        launch {
            // leave room after timeout
            while (true) {
                delay(1000L)

                val lastTime = vertx.eventBus().requestAwait<JsonObject>("game.server.online",
                        jsonObjectOf("action" to "get", "uuid" to uuid)
                ).body().getInteger("time_passed")

                if (lastTime > 45 && playerRoom != "") {
                    vertx.eventBus().send("game.server.room",
                            jsonObjectOf("action" to "leave", "uuid" to uuid))
                    playerRoom = ""
                }

                if (lastTime > 60) {
                    println("player: time out to exit player $uuid")
                    consumer.unregisterAwait()
                    break
                }
            }
        }

        for (msg in channel) {
            vertx.eventBus().send("game.server.online",
                    jsonObjectOf("action" to "set", "uuid" to uuid))

            val request = msg.body()
            println("player: $request")
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
                "reset" -> {
                    vertx.eventBus().send("game.server.room",
                            jsonObjectOf("action" to "reset", "uuid" to uuid))
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