/*
 * Copyright (c) 2017 ATS Advanced Telematic Systems GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.advancedtelematic.ota.deviceregistry.daemon

import akka.Done
import com.advancedtelematic.libats.messaging_datatype.Messages.DeviceEventMessage
import com.advancedtelematic.ota.deviceregistry.db.EventJournal
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class DeviceEventListener()(implicit val db: Database, ec: ExecutionContext)
    extends (DeviceEventMessage => Future[Done]) {

  private[this] val journal = new EventJournal()(db, ec)

  override def apply(message: DeviceEventMessage): Future[Done] =
    journal.recordEvent(message.event).map(_ => Done)
}