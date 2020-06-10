package server

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.ext.bridge.permittedOptionsOf
import io.vertx.kotlin.ext.web.handler.sockjs.bridgeOptionsOf
import io.vertx.kotlin.ext.web.handler.sockjs.sockJSHandlerOptionsOf

class ServerVerticle : CoroutineVerticle() {
    override suspend fun start() {
        super.start()

        val sockJSHandler = SockJSHandler.create(vertx,
                sockJSHandlerOptionsOf(heartbeatInterval = 1000))

        val permitAddress = permittedOptionsOf(addressRegex = "${clentPrefix.replace(".", "\\.")}.*")
        val bridgeOption = bridgeOptionsOf(
                inboundPermitted = listOf(permitAddress),
                outboundPermitted = listOf(permitAddress)
        )

        val router = Router.router(vertx)
        router.mountSubRouter("/eventbus", sockJSHandler.bridge(bridgeOption))

        val server = vertx.createHttpServer()
        server.requestHandler(router).listenAwait(8080)
    }

    override suspend fun stop() {

        super.stop()
    }
}