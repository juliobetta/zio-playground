package com.example.zio

import com.example.zio.domain.User
import com.example.zio.services.{MailerServiceLive, SubscriptionService, SubscriptionServiceLive, UserServiceLive}
import java.util.UUID
import zio.*

// use this project as reference, since it's using module pattern 2.0
// https://github.com/josdirksen/zio-playground

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
  override def run(args: List[String]) = {
    val square: URIO[Int, Int] =
      for {
        env <- ZIO.environment[Int]
      } yield env * env

    val result: UIO[Int] = square.provide(42)

    // *> operator is an alias for the zipRight function, and let us concatenate the execution of two or more
    // effects not depending on each other.
    (
      result.debug *>
      FibersExample.app1 *>
      FibersExample.app2 *>
      subscriptionProgram
    )
      .injectCustom(SubscriptionServiceLive.env)
      .exitCode
  }

    val subscriptionProgram = SubscriptionService.subscribe("Julio", "juliobetta@gmail.com")

    // ALTERNATIVE:
    // val mainApp = subscriptionProgram
    //  .injectCustom(
    //    UserServiceLive.layer,
    //    SubscriptionServiceLive.layer,
    //    MailerServiceLive.layer
    //  )
    // ... and then use `mainApp` in the `run` method
}
