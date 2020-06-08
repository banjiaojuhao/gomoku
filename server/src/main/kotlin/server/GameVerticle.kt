package server

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class GameVerticle : CoroutineVerticle() {
    override suspend fun start() {
        super.start()

        launch {
            game()
        }
    }

    override suspend fun stop() {


        super.stop()
    }

    private suspend fun game() {
        val consumer = vertx.eventBus().consumer<JsonObject>(eventbusAddress)
        val channel = consumer.toChannel(vertx)

        val locker = Mutex()
        val uuidList = hashSetOf<String>()
        val nicknameMap = hashMapOf<String, String>()
        var playerNo = 0

        for (msg in channel) {
            val request = msg.body()
            val action = request.getString("action", "no action field")
            when (action) {
                "new uuid" -> {
                    do {
                        val newUUID = UUID.randomUUID().toString()
                        val success = locker.withLock {
                            if (newUUID in uuidList) {
                                false
                            } else {
                                uuidList.add(newUUID)
                                val nickname = "player $playerNo"
                                nicknameMap[newUUID] = nickname
                                playerNo++
                                msg.reply(jsonObjectOf(
                                        "action" to "new uuid",
                                        "uuid" to newUUID,
                                        "nickname" to nickname
                                ))
                                true
                            }
                        }
                    } while (!success)
                }
                else -> {
                    msg.reply(jsonObjectOf(
                            "action" to "error",
                            "msg" to "wrong action $action"
                    ))
                }
            }
        }
    }
}