package io.cloverex.tiny

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.config.getConfigAwait
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * @author Lin Yang <geekya215@gmail.com>
 */
class MainVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val configuration = ConfigRetriever
      .create(vertx, ConfigRetrieverOptions().addStore(ConfigStoreOptions().apply {
        format = "yaml"
        type = "file"
        config = json { obj("path" to "config.yml") }
      })).getConfigAwait()

    vertx.deployVerticleAwait(
      "io.cloverex.tiny.HttpServerVerticle",
      DeploymentOptions().setConfig(configuration.getJsonObject("server"))
    )

    vertx.deployVerticleAwait(
      "io.cloverex.tiny.DatabaseVerticle",
      DeploymentOptions().setConfig(configuration.getJsonObject("postgres"))
    )
  }
}
