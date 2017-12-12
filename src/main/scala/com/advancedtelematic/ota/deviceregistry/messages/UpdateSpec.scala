/*
 * Copyright (c) 2017 ATS Advanced Telematic Systems GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.advancedtelematic.ota.deviceregistry.messages

import java.time.Instant
import java.util.UUID

import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.advancedtelematic.ota.deviceregistry.data.Uuid
import com.advancedtelematic.ota.deviceregistry.messages.UpdateStatus.UpdateStatus

object UpdateStatus extends Enumeration {
  type UpdateStatus = Value

  val Pending, InFlight, Canceled, Failed, Finished = Value

  implicit val DecoderInstance = io.circe.Decoder.enumDecoder(UpdateStatus)
  implicit val EncoderInstance = io.circe.Encoder.enumEncoder(UpdateStatus)
}

final case class UpdateSpec(namespace: Namespace,
                            device: Uuid,
                            packageUuid: UUID,
                            status: UpdateStatus,
                            timestamp: Instant = Instant.now())

object UpdateSpec {
  import com.advancedtelematic.libats.codecs.CirceCodecs._

  implicit val DecoderInstance       = io.circe.generic.semiauto.deriveDecoder[UpdateSpec]
  implicit val EncoderInstance       = io.circe.generic.semiauto.deriveEncoder[UpdateSpec]
  implicit val UpdateSpecMessageLike = MessageLike[UpdateSpec](_.device.underlying.value)
}
