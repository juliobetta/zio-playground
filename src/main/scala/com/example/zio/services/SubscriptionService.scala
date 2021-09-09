package com.example.zio.services

import zio._

/**
 * HOW TO USE:
 * @example
 * ```UserSubscription.UserSubscriptionService
    .subscribe("Julio", "juliobetta@email.com")
    .provideLayer(SubscriptionServiceLive.layer)
    .injectCustom(UserServiceLive.layer, MailerServiceLive.layer)```
 */
trait SubscriptionService {
  def subscribe(name: String, email: String): UIO[Unit]
}

object SubscriptionService {
  def subscribe(name: String, email: String): URIO[Has[SubscriptionService], Unit] =
    ZIO.serviceWith[SubscriptionService](_.subscribe(name, email))
}

case class SubscriptionServiceLive(userService: UserService, mailerService: MailerService) extends SubscriptionService {
  override def subscribe(name: String, email: String): UIO[Unit] = for {
    user <- userService.insert(name, email)
    _ <- mailerService.notify(user, s"${user.email} created with success!")
  } yield ()
}

object SubscriptionServiceLive {
  val layer: URLayer[
    Has[UserService] with Has[MailerService],
    Has[SubscriptionService]
  ] = (SubscriptionServiceLive(_, _)).toLayer

  // inject env in the main program! thanks to @adamfraser for the tip :)
  val env = (UserServiceLive.layer ++ MailerServiceLive.layer) >>> SubscriptionServiceLive.layer
}
