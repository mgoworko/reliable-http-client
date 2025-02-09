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
package rhttpc.transport.amqp

import com.rabbitmq.client.{Connection, ConnectionFactory}
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem

import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.CollectionConverters._
import scala.util._

object AmqpConnectionFactory {
  private final val DEFAULT_RETRY_CONFIG = AmqpConnectionRetry(
    count = 10,
    delay = Duration(5, TimeUnit.SECONDS),
  )

  def connect(actorSystem: ActorSystem): Future[Connection] = {
    import actorSystem.dispatcher
    val config = actorSystem.settings.config
    val amqpConfig = AmqpConfig(
      hosts = config.getStringList("hosts").asScala.toSeq,
      port = getOptionFromConfig("port", config, _.getInt(_)),
      virtualHost = getOptionFromConfig("virtualHost", config, _.getString(_)),
      userName = getOptionFromConfig("userName", config, _.getString(_)),
      password = getOptionFromConfig("password", config, _.getString(_)),
      retry = getOptionFromConfig("retry", config, _.getConfig(_)).map { c =>
        AmqpConnectionRetry(
          count = config.getInt("count"),
          delay = FiniteDuration(config.getDuration("delay").toMillis, TimeUnit.MILLISECONDS),
        )
      },
    )
    connect(amqpConfig)
  }

  private def getOptionFromConfig[T](path: String, config: Config, extract: (Config, String) => T): Option[T] = {
    getFirstDefinedFromConfig(List(path), config, extract)
  }

  private def getFirstDefinedFromConfig[T](paths: List[String],
                                           config: Config,
                                           extract: (Config, String) => T): Option[T] = {
    paths
      .find(config.hasPath)
      .map(extract(config, _))
  }

  def connect(config: AmqpConfig)(implicit executionContext: ExecutionContext): Future[Connection] =
    Future {
      blocking {
        val factory = new ConnectionFactory()
        config.port.foreach(factory.setPort)
        config.virtualHost.foreach(factory.setVirtualHost)
        config.userName.foreach(factory.setUsername)
        config.password.foreach(factory.setPassword)
        factory.setAutomaticRecoveryEnabled(true)
        val retryConfig = config.retry.getOrElse(DEFAULT_RETRY_CONFIG)
        retry(n = retryConfig.count, delay = retryConfig.delay.toMillis) {

          Try {
            // Could By IOException or TimeoutException
            val addresses = config.hosts.map(com.rabbitmq.client.Address.parseAddress).toArray
            factory.newConnection(addresses)
          }
        }
      }
    }

  @tailrec
  private def retry[T](n: Int, delay: Long)(fn: => Try[T]): T = {
    fn match {
      case Success(x) => x
      case _ if n > 1 =>
        Thread.sleep(delay)
        retry(n - 1, delay)(fn)
      case Failure(e) => throw e
    }
  }
}

case class AmqpConfig(hosts: Seq[String],
                      port: Option[Int],
                      virtualHost: Option[String] = None,
                      userName: Option[String] = None,
                      password: Option[String] = None,
                      retry: Option[AmqpConnectionRetry] = None)

case class AmqpConnectionRetry(count: Int, delay: FiniteDuration)
