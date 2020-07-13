package io.cloverex.tiny.model

import java.time.LocalDateTime

data class Contest(
  val id: Int,
  val formalName: String,
  val shortName: String,
  val penaltyTime: Short,
  val startTime: LocalDateTime,
  val endTime: LocalDateTime,
  val author: String,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime
)
