package io.cloverex.tiny.common

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
private fun mapPgType(value: Any): Any? = when (value) {
  is LocalDate -> value.format(DateTimeFormatter.ISO_DATE)
  is LocalDateTime -> value.format(DateTimeFormatter.ISO_DATE_TIME)
  Tuple.JSON_NULL -> null
  else -> value
}

fun RowSet<Row>.toJsonObject(): Future<JsonObject> = Promise.promise<JsonObject>().run {
  when (rowCount()) {
    0 -> fail(Exception())
    1 -> {
      val row = iterator().next()
      val json = JsonObject()
      columnsNames().asSequence().forEach {
        json.put(it, mapPgType(row.getValue(it)))
      }
      complete(json)
    }
    else -> fail("row count is greater than one")
  }
  future()
}

suspend fun RowSet<Row>.toJsonObjectAwait(): JsonObject {
  return awaitResult {
    it.handle(this.toJsonObject())
  }
}

fun RowSet<Row>.toJsonArray(): JsonArray = asSequence().map { row ->
  val json = JsonObject()
  columnsNames().asSequence().forEach {
    json.put(it, mapPgType(row.getValue(it)))
  }
  json
}.toList().run { JsonArray(this) }

fun OpenAPI3RouterFactory.addCoroutineHandlerByOperationId(
  operationId: String,
  fn: suspend (RoutingContext) -> Unit
): OpenAPI3RouterFactory {
  addHandlerByOperationId(operationId) { rc ->
    GlobalScope.launch(rc.vertx().dispatcher()) {
      try {
        fn(rc)
      } catch (e: Exception) {
        rc.fail(e)
      }
    }
  }
  return this
}
