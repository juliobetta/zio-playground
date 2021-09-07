package com.example.zio.services

import com.example.zio.domain.User
import java.util.UUID
import zio.*

/**
 * HOW TO USE:
 * @example
 * ```MailerService.notify(User(...), message="...").provideLayer(MailerServiceLive.layer)```
 */
trait MailerService {
  def notify(user: User, message: String): UIO[Unit]
}

object MailerService {
  def notify(user: User, message: String): URIO[Has[MailerService], Unit] =
    ZIO.serviceWith[MailerService](_.notify(user, message))
}

case class MailerServiceLive(console: Console, clock: Clock) extends MailerService {
  override def notify(user: User, message: String): UIO[Unit] = for {
    _ <- console.printLine("Sending notification...").orDie
    _ <- clock.sleep(2.seconds)
    current <- clock.currentDateTime
    _ <- console.printLine(s"${current}: $message").orDie
  } yield ()
}

object MailerServiceLive {
  val layer: URLayer[ZEnv, Has[MailerService]] = (MailerServiceLive(_, _)).toLayer
}


// TODO: we can also implement the test service as:
// case class MailerServiceTest(...) extends MailerService { ... }
// object MailerServiceTest { val layer = ... }

// This is how to implement a service in ZIO 1.0
// Module Pattern 1.0 Example
//object MailerV1 {
//
//  type UserMailerEnv = Has[UserMailer.Service]
//
//  object UserMailer {
//    trait Service {
//      def notify(user: User, message: String): Task[Unit]
//    }
//
//    val live: ZLayer[Any, Nothing, UserMailerEnv] = ZLayer.succeed(
//      new Service {
//        override def notify(user: User, message: String): Task[Unit] = Task {
//          println(s"Sending $message to ${user.email}")
//        }
//      }
//    )
//
//    // provides a higher-level API
//    def notify(user: User, message: String): ZIO[UserMailerEnv, Throwable, Unit] =
//      ZIO.accessZIO(_.get.notify(user, message))
//
//
//    val app = MailerV1.UserMailer
//      .notify(User("Julio", "juliobetta@gmail.com", UUID.randomUUID().toString()), "Something's going on")
//      .provideLayer(MailerV1.UserMailer.live)
//  }
//}
