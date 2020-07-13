package io.cloverex.tiny.service.impl

import io.cloverex.tiny.common.toJsonArray
import io.cloverex.tiny.service.ContestService
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.sqlclient.beginAwait
import io.vertx.kotlin.sqlclient.commitAwait
import io.vertx.kotlin.sqlclient.executeAwait
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.time.LocalDateTime

class ContestServiceImpl(private val connection: SqlConnection) : ContestService {
  override suspend fun getAllContests(pagination: JsonObject): JsonArray {
    val offset = pagination.getInteger("offset")
    val limit = pagination.getInteger("limit")
    return connection.preparedQuery("select id, formal_name, short_name, start_time, end_time from public.contest offset $1 limit $2")
      .executeAwait(Tuple.of(offset, limit))
      .toJsonArray()
  }

  override suspend fun createContest(contest: JsonObject): JsonObject {
    return try {
      val tx = connection.beginAwait()
      connection.preparedQuery("insert into public.contest(formal_name, short_name, penalty_time, start_time, end_time, author) VALUES ($1, $2, $3, $4, $5, $6)")
        .executeAwait(Tuple.of(
          contest.getString("formal_name"),
          contest.getString("short_name"),
          contest.getInteger("penalty_time"),
          LocalDateTime.parse(contest.getString("start_time")),
          LocalDateTime.parse(contest.getString("end_time")),
          contest.getString("author")
        ))
      tx.commitAwait()
      JsonObject().put("status", 201)
    } catch (e: Exception) {
      JsonObject().put("status", 409).put("message", "contest formal name or short name already exists")
    }
  }
}
