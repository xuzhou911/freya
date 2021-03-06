package freya.internal

import cats.effect.{Effect, Timer}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import freya.ExitCodes
import freya.ExitCodes.ReconcilerExitCode
import freya.errors.ParseReconcileError
import freya.models.ResourcesList
import freya.watcher.AbstractWatcher.Action
import freya.watcher.Channels
import freya.watcher.actions.ReconcileAction

import scala.concurrent.duration._

private[freya] class Reconciler[F[_], T, U](
  delay: FiniteDuration,
  channels: Channels[F, T, U],
  currentResources: F[Either[Throwable, ResourcesList[T, U]]]
)(implicit F: Effect[F], T: Timer[F])
    extends LazyLogging {

  def run: F[ReconcilerExitCode] =
    F.suspend {
      for {
        _ <- T.sleep(delay)
        _ <- F.delay(logger.debug("Reconciler is running >>>>"))
        r <- currentResources
        _ <- publish(r)
        ec <- run
      } yield ec
    }.recoverWith {
      case e =>
        F.delay(logger.error("Failed in reconciling loop", e)) *>
            ExitCodes.ReconcileExitCode.pure[F]
    }

  private def publish(resources: Either[Throwable, ResourcesList[T, U]]): F[Unit] =
    resources.fold(
      t => F.delay(logger.error(s"Failed to get current resources", t)),
      _.map {
        case Left((t, resource)) =>
          val action = Left(ParseReconcileError(t, resource))
          putAction(resource.getMetadata.getNamespace, action)
        case Right(resource) =>
          val action = Right(ReconcileAction(resource))
          putAction(resource.metadata.namespace, action)
      }.sequence.void
    )

  private def putAction(namespace: String, action: Action[T, U]) =
    channels.getOrCreateConsumer(namespace).flatMap(_.putAction(action))
}
