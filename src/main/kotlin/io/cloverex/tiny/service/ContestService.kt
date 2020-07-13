package io.cloverex.tiny.service

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

interface ContestService {
  suspend fun getAllContests(pagination: JsonObject): JsonArray

  suspend fun createContest(contest: JsonObject): JsonObject
}
