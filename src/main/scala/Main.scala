import java.util.UUID
import zio.*

case class User(name: String, email: String, id: String)

object Fibers {
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

// ## ZLAYER
// https://blog.rockthejvm.com/structuring-services-with-zio-zlayer/

// Module Pattern 1.0
object MailerV1 {

  type UserMailerEnv = Has[UserMailer.Service]

  object UserMailer {
    trait Service {
      def notify(user: User, message: String): Task[Unit]
    }

    val live: ZLayer[Any, Nothing, UserMailerEnv] = ZLayer.succeed(
      new Service {
        override def notify(user: User, message: String): Task[Unit] = Task {
          println(s"Sending $message to ${user.email}")
        }
      }
    )

    // provides a higher-level API
    def notify(user: User, message: String): ZIO[UserMailerEnv, Throwable, Unit] =
      ZIO.accessZIO(_.get.notify(user, message))


    val app = MailerV1.UserMailer
      .notify(User("Julio", "juliobetta@gmail.com", UUID.randomUUID().toString()), "Something's going on")
      .provideLayer(MailerV1.UserMailer.live)
  }
}

// Module Pattern 2.0
object UserDB {

  trait UserDBService {
    def insert(name: String, email: String): UIO[User]
  }

  object UserDBService {
    def insert(name: String, email: String): URIO[Has[UserDBService], User] =
      ZIO.serviceWith[UserDBService](_.insert(name, email))
  }

  case class UserDBServiceLive(clock: Clock, console: Console) extends UserDBService {
    override def insert(name: String, email: String): UIO[User] = for {
      _ <- clock.sleep(2.seconds)
      id <- ZIO.succeed(UUID.randomUUID().toString)
      _ <- console.printLine(s"User ${email} created! ID -> $id").orDie
    } yield User(id, name, email)
  }

  object UserDBServiceLive {
    val layer: URLayer[Has[Clock] with Has[Console], Has[UserDBService]] = (UserDBServiceLive(_, _)).toLayer
  }

  // just to test the service
  val app = UserDB.UserDBService
    .insert("Julio", "juliobetta@gmail.com")
    .provideLayer(UserDB.UserDBServiceLive.layer)
}

object MailerV2 {

  trait UserMailerService {
    def notify(user: User, message: String): UIO[Unit]
  }

  object UserMailerService {
    def notify(user: User, message: String): URIO[Has[UserMailerService], Unit] =
      ZIO.serviceWith[UserMailerService](_.notify(user, message))
  }

  case class UserMailerServiceLive(console: Console, clock: Clock) extends UserMailerService {
    override def notify(user: User, message: String): UIO[Unit] = for {
      _ <- console.printLine("Sending notification...").orDie
      _ <- clock.sleep(2.seconds)
      current <- clock.currentDateTime
      _ <- console.printLine(s"$current: $message").orDie
    } yield ()
  }

  object UserMailerServiceLive {
    val layer: URLayer[Has[Console] with Has[Clock], Has[UserMailerService]] = (UserMailerServiceLive(_, _)).toLayer
  }

  // just to test the service
  val app = UserMailerService
    .notify(User("Julio", "juliobetta@gmail.com", "12345"), "Something's going on")
    .provideLayer(MailerV2.UserMailerServiceLive.layer)
}

object UserSubscription {

  trait UserSubscriptionService {
    def subscribe(name: String, email: String): UIO[Unit]
  }

  object UserSubscriptionService {
    def subscribe(name: String, email: String): ZIO[Has[UserSubscriptionService], Nothing, Unit] =
      ZIO.serviceWith[UserSubscriptionService](_.subscribe(name, email))
  }

  case class UserSubscriptionServiceLive(userDB: UserDB.UserDBService, userMailer: MailerV2.UserMailerService) extends UserSubscriptionService {
    override def subscribe(name: String, email: String): UIO[Unit] = for {
      user <- userDB.insert(name, email)
      _ <- userMailer.notify(user, s"User ${user.email} created with success")
    } yield()
  }

  object UserSubscriptionServiceLive {
    val layer: URLayer[
      Has[UserDB.UserDBService] with Has[MailerV2.UserMailerService],
      Has[UserSubscription.UserSubscriptionService]
    ] = (UserSubscriptionServiceLive(_, _)).toLayer
  }

  val app = UserSubscription.UserSubscriptionService
    .subscribe("Julio", "juliobetta@email.com")
    .provideLayer(UserSubscriptionServiceLive.layer)
    .injectCustom(UserDB.UserDBServiceLive.layer, MailerV2.UserMailerServiceLive.layer)
}

object Main extends App {
  // *> operator is an alias for the zipRight function, and let us concatenate the execution of two
  // effects not depending on each other.
  override def run(args: List[String]) = (Fibers.app1 *> Fibers.app2 *> UserSubscription.app).exitCode

  object Hello {
    val app = for {
      _ <- Console.printLine("Hello! What's your name?")
      name <- Console.readLine
      _ <- Console.printLine(s"Hello, $name")
    } yield ()
  }
}
