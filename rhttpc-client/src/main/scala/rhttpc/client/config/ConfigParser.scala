/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rhttpc.client.config

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import rhttpc.client.config.ConfigParserUtils._
import rhttpc.client.proxy.{BackoffRetry, FailureResponseHandleStrategyChooser, HandleAll, SkipAll}
import rhttpc.transport.QueueType

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Try}

object ConfigParser {
  def parse(system: ActorSystem): RhttpcConfig = {
    parse(system.settings.config, "rhttpc")
  }

  def parse(config: Config, path: String): RhttpcConfig = {
    val configAtPath = if (path == ".") config else config.getConfig(path)
    RhttpcConfig(
      queuesPrefix = configAtPath.getString("queuesPrefix"),
      batchSize = configAtPath.getInt("batchSize"),
      parallelConsumers = configAtPath.getInt("parallelConsumers"),
      retryStrategy = RetryStrategyValueReader.getRetryStrategy(configAtPath, "retryStrategy"),
      queueType = QueueTypeValueReader.getQueueType(configAtPath, "queueType"),
    )
  }
}

object RetryStrategyValueReader {
  def getRetryStrategy(config: Config, path: String): FailureResponseHandleStrategyChooser = {
    Try(config.getString(path)) match {
      case Success("handle-all") => HandleAll
      case Success("skip-all") => SkipAll
      case _ =>
        BackoffRetry(
          initialDelay = toFiniteDuration(config.getDuration(s"$path.initialDelay")),
          multiplier = BigDecimal(config.getDouble(s"$path.multiplier")),
          maxRetries = config.getInt(s"$path.maxRetries"),
          deadline = getOptionFromConfig(s"$path.deadline", config, _.getDuration(_)).map(toFiniteDuration),
        )
    }
  }
}

object QueueTypeValueReader {
  def getQueueType(config: Config, path: String): QueueType = {
    getOptionFromConfig(path, config, _.getString(_)).fold[QueueType](QueueType.ClassicQueue) {
      case "classic" => QueueType.ClassicQueue
      case "quorum" => QueueType.QuorumQueue
      case other =>
        throw InvalidConfigValueException(s"Invalid value as QueueType=[$other]. Should be one of: classic, quorum.")
    }
  }
}

private object ConfigParserUtils {
  def toFiniteDuration(duration: Duration): FiniteDuration =
    FiniteDuration(duration.toMillis, TimeUnit.MILLISECONDS)

  def getOptionFromConfig[T](path: String, config: Config, extract: (Config, String) => T): Option[T] = {
    getFirstDefinedFromConfig(List(path), config, extract)
  }

  def getFirstDefinedFromConfig[T](paths: List[String], config: Config, extract: (Config, String) => T): Option[T] = {
    paths
      .find(config.hasPath)
      .map(extract(config, _))
  }

}

case class RhttpcConfig(queuesPrefix: String,
                        batchSize: Int,
                        parallelConsumers: Int,
                        retryStrategy: FailureResponseHandleStrategyChooser,
                        queueType: QueueType)

final case class InvalidConfigValueException(message: String) extends Exception(message)
