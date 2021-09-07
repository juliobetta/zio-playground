package com.example.zio.services

import com.example.zio.domain.User
import java.util.UUID
import zio.*

/**
 * HOW TO USE:
 * @example 
 * ```UserService.insert("Julio", "juliobetta@gmail.com").provideLayer(UserServiceLive.layer)```
 */
trait UserService {
  def insert(name: String, email: String): UIO[User]
}

object UserService {
  def insert(name: String, email: String): URIO[Has[UserService], User] =
    ZIO.serviceWith[UserService](_.insert(name, email))
}

case class UserServiceLive(clock: Clock, console: Console) extends UserService {
  override def insert(name: String, email: String): UIO[User] = for {
    _ <- clock.sleep(2.seconds) // simulate delay
    id <- ZIO.succeed(UUID.randomUUID().toString)
    _ <- console.printLine(s"User $email created! ID -> $id").orDie
  } yield User(id, name, email)
}

object UserServiceLive {
  // OBS: ZEnv has common services such as Console and Clock

  /**
   * this higher-level API method is used to simplify the lifting the service implementation to ZLayer
   */
  def layer: URLayer[ZEnv, Has[UserService]] = (UserServiceLive(_, _)).toLayer
}
