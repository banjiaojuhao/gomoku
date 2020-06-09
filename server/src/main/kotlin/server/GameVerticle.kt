package server

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.launch
import java.util.*

class GameVerticle : CoroutineVerticle() {
    override suspend fun start() {
        super.start()

        launch {
            uuid()
            nickname()
            online()
            room()
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
                        val inRoom = vertx.eventBus().requestAwait<JsonObject>("game.server.match.$roomId",
                                jsonObjectOf("action" to "enter", "uuid" to uuid))
                                .body().getBoolean("in_room")
                        msg.reply(jsonObjectOf("in_room" to inRoom))
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

    private suspend fun match(roomId: String) {
        val consumer = vertx.eventBus().consumer<JsonObject>("game.server.match.$roomId")
        val channel = consumer.toChannel(vertx)

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action")
            when(action) {
                "enter" -> {

                }
                "leave" -> {

                }
            }
        }
    }

    private val noAction = "no action field in json"

    private suspend fun login() {
        val consumer = vertx.eventBus().consumer<JsonObject>(eventbusAddress)
        val channel = consumer.toChannel(vertx)


        for (msg in channel) {
            val request = msg.body()
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
                    msg.reply(jsonObjectOf(
                            "action" to "new uuid",
                            "uuid" to uuid,
                            "nickname" to name
                    ))
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
        val consumer = vertx.eventBus().consumer<JsonObject>("$eventbusAddress.$uuid")
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
                    vertx.eventBus().requestAwait<JsonObject>("game.service.nickname",
                            jsonObjectOf("action" to "set", "name" to name))
                }
                "heart beat" -> {
                    // no-op
                }
                "enter room" -> {
                    val roomId = request.getString("id")
                    playerRoom = roomId
                    val inRoom = vertx.eventBus().requestAwait<JsonObject>("game.server.room",
                            jsonObjectOf("action" to "enter", "uuid" to uuid, "id" to roomId))
                            .body().getBoolean("in_room")
                    msg.reply(jsonObjectOf(
                            "action" to "enter room",
                            "in_room" to inRoom
                    ))
                }
                "leave room" -> {
                    vertx.eventBus().send("game.server.room",
                            jsonObjectOf("action" to "leave", "uuid" to uuid))
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