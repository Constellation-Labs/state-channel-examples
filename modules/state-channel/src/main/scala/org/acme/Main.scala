package org.acme

import java.security.KeyPair

import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}

import org.tessellation.dag.dagSharedKryoRegistrar
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary
import org.tessellation.keytool.KeyStoreUtils
import org.tessellation.kryo.KryoSerializer
import org.tessellation.security.SecurityProvider
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed
import org.tessellation.shared.sharedKryoRegistrar

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    SecurityProvider
      .forAsync[IO]
      .flatMap { sp =>
        KryoSerializer.forAsync[IO](sharedKryoRegistrar ++ dagSharedKryoRegistrar).map((sp, _))
      }
      .use { res =>
        implicit val (securityProvider, kryoSerializer) = res

        def readKeypair: IO[KeyPair] =
          KeyStoreUtils
            .readKeyPairFromStore[IO]("./sc-key.p12", "alias", "password".toCharArray(), "password".toCharArray())

        for {
          keypair <- readKeypair

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
        } yield ExitCode.Success
      }
}
