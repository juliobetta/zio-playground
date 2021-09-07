package com.example.zio

import com.example.zio.domain.User
import com.example.zio.services.{MailerServiceLive, SubscriptionService, SubscriptionServiceLive, UserServiceLive}
import java.util.UUID
import zio.*

object FibersExample {
  // https://blog.rockthejvm.com/zio-fibers/
  // https://zio.dev/docs/overview/overview_basic_concurrency

  // combining fibers
  val app1 = for {
    fiber1 <- IO.succeed("Hi!").fork
    fiber2 <- IO.succeed("Bye!").fork
    fiber = fiber1.zip(fiber2)
    tuple <- fiber.join
    _ <- Console.printLine(tuple)
  } yield tuple

  // handling failures
  val app2 = for {
    fiber1 <- IO.fail("Uh oh!").fork
    fiber2 <- IO.succeed("Hurray!").fork
    fiber = fiber1.orElse(fiber2)
    tuple <- fiber.join
    _ <- Console.printLine(tuple)
  } yield tuple
}

object HelloExample {
  val app = for {
    _ <- Console.printLine("Hello! What's your name?")
    name <- Console.readLine
    _ <- Console.printLine(s"Hello, $name")
  } yield ()
}

object Main extends App {
  // *> operator is an alias for the zipRight function, and let us concatenate the execution of two
  // effects not depending on each other.
  override def run(args: List[String]) = (FibersExample.app1 *> FibersExample.app2 *> subscriptionApp).exitCode

  val subscriptionLayer = (UserServiceLive.layer ++ MailerServiceLive.layer) >>> SubscriptionServiceLive.layer

  val subscriptionApp = SubscriptionService
    .subscribe("Julio", "juliobetta@email.com")
    // the good thing about that is that we could provide a Test layer, instead of "Live"
    .provideLayer(subscriptionLayer)
    // Another way to inject the dependencies
    //.provideLayer(SubscriptionServiceLive.layer)
    //.injectCustom(UserServiceLive.layer, MailerServiceLive.layer)
}
