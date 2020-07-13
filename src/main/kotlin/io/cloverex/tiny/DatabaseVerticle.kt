package io.cloverex.tiny

import io.cloverex.tiny.service.impl.ContestServiceImpl
import io.cloverex.tiny.service.impl.UserServiceImpl
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.sqlclient.getConnectionAwait
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
class DatabaseVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val connection = PgPool.pool(
      vertx,
      PgConnectOptions(config.getJsonObject("connect")),
      PoolOptions(config.getJsonObject("pool"))
    ).getConnectionAwait()

    val userService = UserServiceImpl(connection)
    val contestService = ContestServiceImpl(connection)

    vertx.eventBus().localConsumer<JsonObject>("user.login") { msg ->
      GlobalScope.launch(vertx.dispatcher()) {
        msg.reply(userService.login(msg.body()))
      }
    }

    vertx.eventBus().localConsumer<JsonObject>("user.register") { msg ->
      GlobalScope.launch(vertx.dispatcher()) {
        msg.reply(userService.register(msg.body()))
      }
    }

    vertx.eventBus().localConsumer<JsonObject>("contest.getAll") { msg ->
      GlobalScope.launch(vertx.dispatcher()) {
        msg.reply(contestService.getAllContests(msg.body()))
      }
    }

    vertx.eventBus().localConsumer<JsonObject>("contest.create") { msg ->
      GlobalScope.launch(vertx.dispatcher()) {
        msg.reply(contestService.createContest(msg.body()))
      }
    }
  }
}
