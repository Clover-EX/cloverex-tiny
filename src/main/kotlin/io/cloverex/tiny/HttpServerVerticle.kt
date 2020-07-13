package io.cloverex.tiny

import io.cloverex.tiny.common.addCoroutineHandlerByOperationId
import io.cloverex.tiny.common.addSecurityCoroutineHandler
import io.cloverex.tiny.constant.Role
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.jwt.JWTCredentials
import io.vertx.ext.web.Router
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.ext.web.api.validation.ValidationException
import io.vertx.ext.web.handler.ErrorHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.auth.jwt.authenticateAwait

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
class HttpServerVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val routerFactory = OpenAPI3RouterFactory.create(vertx, "api.yml").await()

    val provider = JWTAuth.create(vertx, JWTAuthOptions(json {
      obj(
        "keyStore" to obj(
          "type" to "jceks",
          "path" to "keystore.jceks",
          "password" to "secret"
        )
      )
    }))

    routerFactory.addCoroutineHandlerByOperationId("post-login") { rc ->
      val user = rc.bodyAsJson
      val username = user.getString("username")
      val res = vertx.eventBus().requestAwait<JsonObject>("user.login", user).body()
      rc.response().statusCode = res.getInteger("status")
      if (res.getInteger("status") == 200) {
        val claims = JsonObject().put("username", username).put("role", res.getInteger("role"))
        val accessToken = provider.generateToken(claims, JWTOptions().setExpiresInMinutes(24 * 60))
        rc.response().end(JsonObject().put("access_token", accessToken).encodePrettily())
      } else {
        rc.response().end(res.encodePrettily())
      }
    }

    routerFactory.addCoroutineHandlerByOperationId("post-register") { rc ->
      val user = rc.bodyAsJson
      val res = vertx.eventBus().requestAwait<JsonObject>("user.register", user).body()
      rc.response().statusCode = res.getInteger("status")
      if (res.getInteger("status") == 201) {
        rc.response().end()
      } else {
        rc.response().end(res.encodePrettily())
      }
    }

    routerFactory.addCoroutineHandlerByOperationId("get-contests") { rc ->
      val offset = rc.request().getParam("offset").toInt()
      val limit = rc.request().getParam("limit").toInt()
      val pagination = JsonObject().put("offset", offset).put("limit", limit)
      val res = vertx.eventBus().requestAwait<JsonArray>("contest.getAll", pagination).body()
      rc.response().end(res.encodePrettily())
    }

    routerFactory
      .addSecurityCoroutineHandler("jwt") { rc ->
        val authorization = rc.request().headers().get("authorization")
        if (authorization == null) {
          rc.response().statusCode = 401
          rc.response().end(JsonObject().put("status", 401).put("message", "jwt token not founded").encodePrettily())
          return@addSecurityCoroutineHandler
        }
        val idx = authorization.indexOf(' ')

        when {
          idx <= 0 -> {
            rc.response().statusCode = 400
            rc.response().end(JsonObject().put("status", 400).put("message", "invalid bearer token").encodePrettily())
          }
          authorization.substring(0, idx).toLowerCase() != "bearer" -> {
            rc.response().statusCode = 401
            rc.response().end(JsonObject().put("status", 401).put("message", "jwt token authenticate failed").encodePrettily())
          }
          else -> {
            val user = provider.authenticateAwait(JWTCredentials().setJwt(authorization.substring(idx + 1)))
            val role = user.principal().getInteger("role")
            if (role == Role.Admin.ordinal) {
              rc.next()
            } else {
              rc.response().statusCode = 403
              rc.response().end(JsonObject().put("status", 403).put("message", "no permission to operate").encodePrettily())
            }
          }
        }
      }
      .addCoroutineHandlerByOperationId("post-contests") { rc ->
        val contest = rc.bodyAsJson
        contest.put("author", "tom")
        val res = vertx.eventBus().requestAwait<JsonObject>("contest.create", contest).body()
        rc.response().statusCode = res.getInteger("status")
        if (res.getInteger("status") == 201) {
          rc.response().end()
        } else {
          rc.response().end(res.encodePrettily())
        }
      }

    val mainRouter = Router.router(vertx).apply {
      route()
        .handler(LoggerHandler.create())
        .handler(ResponseTimeHandler.create())
        .handler(TimeoutHandler.create())
        .failureHandler { frc ->
          if (frc.failure() is ValidationException) {
            frc.response().statusCode = 400
            frc.response().end(JsonObject().put("status", 400).put("message", frc.failure().message).encodePrettily())
          } else {
            frc.next()
          }
        }
        .failureHandler(ErrorHandler.create())
    }

    val subRouter = routerFactory.router

    mainRouter.mountSubRouter("/api/v1/", subRouter)

    vertx
      .createHttpServer()
      .requestHandler(mainRouter)
      .listen(config.getInteger("port"))
  }
}
