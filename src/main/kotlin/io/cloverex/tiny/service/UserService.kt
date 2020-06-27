package io.cloverex.tiny.service

import io.vertx.core.json.JsonObject

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
interface UserService {
  suspend fun login(user: JsonObject): JsonObject

  suspend fun register(user: JsonObject): JsonObject
}
