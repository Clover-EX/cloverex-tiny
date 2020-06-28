package io.cloverex.tiny.model

import java.time.LocalDateTime

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
data class User(
  val id: Int,
  val username: String,
  val realName: String,
  val email: String,
  val password: String,
  val role: Short,
  val enable: Boolean,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime
)
