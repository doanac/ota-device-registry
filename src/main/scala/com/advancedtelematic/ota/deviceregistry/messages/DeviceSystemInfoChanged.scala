/*
 * Copyright (c) 2017 ATS Advanced Telematic Systems GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.advancedtelematic.ota.deviceregistry.messages

import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.messaging_datatype.MessageLike
import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceId

final case class DeviceSystemInfoChanged(namespace: Namespace, uuid: DeviceId)

object DeviceSystemInfoChanged {
  import cats.syntax.show._
  import com.advancedtelematic.libats.codecs.CirceCodecs._

  implicit val MessageLikeInstance = MessageLike.derive[DeviceSystemInfoChanged](_.uuid.show)
}
