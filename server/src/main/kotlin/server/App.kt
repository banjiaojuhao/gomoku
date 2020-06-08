package server

import io.vertx.core.Vertx
import io.vertx.kotlin.core.closeAwait
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.undeployAwait
import io.vertx.kotlin.coroutines.awaitEvent
import kotlinx.coroutines.runBlocking
import sun.misc.Signal


fun main(args: Array<String>) = runBlocking<Unit> {
    val vertx = Vertx.vertx()
    val mainVerticleId = vertx.deployVerticleAwait(ServerVerticle())
    val gameVerticleId = vertx.deployVerticleAwait(GameVerticle())

    // wait for ctrl-c
    awaitEvent<Unit> { handler ->
        Signal.handle(Signal("INT")) {
            handler.handle(Unit)
        }
    }

    println("stop program")
    vertx.undeployAwait(gameVerticleId)
    vertx.undeployAwait(mainVerticleId)
    vertx.closeAwait()
}
