package io.cloverex.tiny.service.impl

import io.cloverex.tiny.common.toJsonObjectAwait
import io.cloverex.tiny.service.UserService
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.sqlclient.beginAwait
import io.vertx.kotlin.sqlclient.commitAwait
import io.vertx.kotlin.sqlclient.executeAwait
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.mindrot.jbcrypt.BCrypt

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
class UserServiceImpl(private val connection: SqlConnection) : UserService {
  override suspend fun login(user: JsonObject): JsonObject {
    val username = user.getString("username")
    val password = user.getString("password")
    try {
      val obj = connection
        .preparedQuery("select password, role, enable from public.user where username = $1 limit 1")
        .executeAwait(Tuple.of(username))
        .toJsonObjectAwait()
      val hashpw = obj.getString("password")
      val role = obj.getInteger("role")
      val enable = obj.getBoolean("enable")
      return if (BCrypt.checkpw(password, hashpw)) {
        if (enable) {
          JsonObject().put("status", 200).put("role", role)
        } else {
          JsonObject().put("status", 403).put("message", "user is disable now")
        }
      } else {
        JsonObject().put("status", 401).put("message", "username do not match password")
      }
    } catch (e: Exception) {
      return JsonObject().put("status", 401).put("message", "username do not match password")
    }
  }

  override suspend fun register(user: JsonObject): JsonObject {
    val username = user.getString("username")
    val realName = user.getString("real_name")
    val email = user.getString("email")
    val password = user.getString("password")
    val hashpw = BCrypt.hashpw(password, BCrypt.gensalt(10))
    return try {
      val tx = connection.beginAwait()
      connection
        .preparedQuery("insert into public.user(username, real_name, email,  password) values ($1, $2, $3, $4)")
        .executeAwait(Tuple.of(username, realName, email, hashpw))
      tx.commitAwait()
      JsonObject().put("status", 201)
    } catch (e: Exception) {
      JsonObject().put("status", 409).put("message", "username or email already exist")
    }
  }
}
