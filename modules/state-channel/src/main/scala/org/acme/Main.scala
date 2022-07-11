package org.acme

import java.security.KeyPair

import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}

import org.http4s.Method.POST
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Request, Uri}
import org.tessellation.dag.dagSharedKryoRegistrar
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary
import org.tessellation.keytool.KeyStoreUtils
import org.tessellation.kryo.KryoSerializer
import org.tessellation.security.SecurityProvider
import org.tessellation.security.hash.Hash
import org.tessellation.security.key.ops._
import org.tessellation.security.signature.Signed
import org.tessellation.shared.sharedKryoRegistrar

object Main extends IOApp {

  val NETWORK_API = "testnet.lb.constellationnetwork.io:9000"

  def run(args: List[String]): IO[ExitCode] =
    SecurityProvider
      .forAsync[IO]
      .flatMap { implicit sp =>
        KryoSerializer.forAsync[IO](sharedKryoRegistrar ++ dagSharedKryoRegistrar).flatMap { implicit kryo =>
          EmberClientBuilder.default[IO].build.flatMap { httpClient =>
            def readKeypair: IO[KeyPair] =
              KeyStoreUtils
                .readKeyPairFromStore[IO]("./sc-key.p12", "alias", "password".toCharArray(), "password".toCharArray())

            def program =
              for {
                keypair <- readKeypair
                address = keypair.getPublic.toAddress

                _ <- Console[IO].println(s"Address: ${address}")

                stateChannelData = "hello world"
                stateChannelDataBinary = stateChannelData.getBytes
                stateChannelSnapshot = StateChannelSnapshotBinary(
                  lastSnapshotHash = Hash.empty,
                  content = stateChannelDataBinary
                )

                signedStateChannelSnapshot <- Signed
                  .forAsyncKryo[IO, StateChannelSnapshotBinary](stateChannelSnapshot, keypair)

                _ <- Console[IO].println(s"State channel data: $stateChannelData")
                _ <- Console[IO].println(s"State channel snapshot: $stateChannelSnapshot")
                _ <- Console[IO].println(s"Signature: ${signedStateChannelSnapshot.proofs.head}")

                hashedStateChannelSnapshot <- signedStateChannelSnapshot.toHashed[IO]

                secondStateChannelSnapshot = StateChannelSnapshotBinary(
                  lastSnapshotHash = hashedStateChannelSnapshot.hash,
                  content = "hello world second".getBytes
                )

                request = Request[IO](
                  method = POST,
                  uri = Uri.unsafeFromString(s"http://${NETWORK_API}/state-channels/${address}/snapshot")
                ).withEntity(signedStateChannelSnapshot)
                result <- httpClient.successful(request)

                _ <- if (result) {
                  Console[IO].println(s"State Channel Snapshot sent successfully")
                } else {
                  Console[IO].println(s"Error while sending the State Channel Snapshot")
                }
              } yield ExitCode.Success

            Resource.liftK[IO](
              program.handleErrorWith(
                error => Console[IO].println(s"An error occured: ${error.getMessage}").as(ExitCode.Error)
              )
            )
          }
        }
      }
      .use(IO.pure)
}
