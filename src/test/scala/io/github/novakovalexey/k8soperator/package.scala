package io.github.novakovalexey

import cats.effect.{CancelToken, ContextShift, ExitCode, IO}
import io.fabric8.kubernetes.api.model.ConfigMap
import io.github.novakovalexey.k8soperator.internal.resource.ConfigMapParser
import org.scalacheck.{Arbitrary, Gen}

import scala.concurrent.ExecutionContext

package object k8soperator {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit lazy val arbInfoClass: Arbitrary[Kerb] = Arbitrary(Gens.krb2)
  implicit lazy val arbBooleab: Arbitrary[Boolean] = Arbitrary(Gen.oneOf(true, false))

  val prefix = "io.github.novakov-alexey"

  def startOperator(io: IO[ExitCode]): CancelToken[IO] =
    io.unsafeRunCancelable {
      case Right(ec) =>
        println(s"Operator stopped with exit code: $ec")
      case Left(t) =>
        println("Failed to start operator")
        t.printStackTrace()
    }

  def parseCM(parser: ConfigMapParser, cm: ConfigMap): Kerb =
    parser.parseCM(classOf[Kerb], cm).getOrElse(sys.error("Error when transforming ConfigMap to Krb2"))._1
}