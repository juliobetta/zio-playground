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
  override def run(args: List[String]) = (FibersExample.app1 *> FibersExample.app2 *> mainApp).exitCode

  val subscriptionProgram: URIO[Has[SubscriptionService], Unit] =
    SubscriptionService.subscribe("Julio", "juliobetta@gmail.com")

  val mainApp = subscriptionProgram
    // the order doesn't matter
    // tip: try commenting out some layer. the error message is awesome!
    .injectCustom(
      UserServiceLive.layer,
      SubscriptionServiceLive.layer,
      MailerServiceLive.layer
    )
}
