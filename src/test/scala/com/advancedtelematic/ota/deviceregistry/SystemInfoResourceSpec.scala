/*
 * Copyright (c) 2017 ATS Advanced Telematic Systems GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.advancedtelematic.ota.deviceregistry

import com.advancedtelematic.ota.deviceregistry.db.SystemInfoRepository.removeIdNrs
import io.circe.Json
import org.scalacheck.Shrink
import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceId
import com.advancedtelematic.ota.deviceregistry.data.DataType.DeviceT

class SystemInfoResourceSpec extends ResourcePropSpec {
  import akka.http.scaladsl.model.StatusCodes._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  property("GET /system_info request fails on non-existent device") {
    forAll { (uuid: DeviceId, json: Json) =>
      fetchSystemInfo(uuid) ~> route ~> check { status shouldBe NotFound }
      createSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound }
      updateSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound }
    }
  }

  property("GET /system_info/network returns 404 NotFound on non-existent device") {
    forAll { uuid: DeviceId =>
      fetchNetworkInfo(uuid) ~> route ~> check { status shouldBe NotFound }
    }
  }

  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  property("GET /system_info/network returns empty strings if network info was not reported") {
    forAll { (device: DeviceT, json: Option[Json]) =>
      val uuid = createDeviceOk(device)

      json.foreach { sysinfo =>
        createSystemInfo(uuid, removeIdNrs(sysinfo)) ~> route ~> check {
          status shouldBe Created
        }
      }

      fetchNetworkInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json = responseAs[Json]
        json.hcursor.get[String]("local_ipv4").toOption should equal(Some(""))
        json.hcursor.get[String]("mac").toOption should equal(Some(""))
        json.hcursor.get[String]("hostname").toOption should equal(Some(""))
      }
    }
  }

  property("GET /system_info return empty if device have not set system_info") {
    forAll { device: DeviceT =>
      val uuid = createDeviceOk(device)

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json = responseAs[Json]

        json shouldBe Json.obj()
      }
    }
  }

  property("GET system_info after POST should return what was posted.") {
    forAll { (device: DeviceT, json0: Json) =>
      val uuid  = createDeviceOk(device)
      val json1: Json = removeIdNrs(json0)

      createSystemInfo(uuid, json1) ~> route ~> check {
        status shouldBe Created
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        json1 shouldBe removeIdNrs(json2)
      }
    }
  }

  property("GET system_info after PUT should return what was updated.") {
    forAll { (device: DeviceT, json1: Json, json2: Json) =>
      val uuid = createDeviceOk(device)

      createSystemInfo(uuid, json1) ~> route ~> check {
        status shouldBe Created
      }

      updateSystemInfo(uuid, json2) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json3: Json = responseAs[Json]
        json2 shouldBe removeIdNrs(json3)
      }
    }
  }

  property("PUT system_info if not previously created should create it.") {
    forAll { (device: DeviceT, json: Json) =>
      val uuid = createDeviceOk(device)

      updateSystemInfo(uuid, json) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        json shouldBe removeIdNrs(json2)
      }
    }
  }

  property("system_info adds unique numbers for each json-object") {
    def countObjects(json: Json): Int = json.arrayOrObject(
      0,
      x => x.map(countObjects).sum,
      x => x.toList.map { case (_, v) => countObjects(v) }.sum + 1
    )

    def getField(field: String)(json: Json): Seq[Json] =
      json.arrayOrObject(List(),
                         _.flatMap(getField(field)),
                         x =>
                           x.toList.flatMap {
                             case (i, v) if i == field => List(v)
                             case (_, v)               => getField(field)(v)
                         })

    forAll { (device: DeviceT, json0: Json) =>
      val uuid = createDeviceOk(device)
      val json       = removeIdNrs(json0)

      updateSystemInfo(uuid, json) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val retJson = responseAs[Json]
        json shouldBe removeIdNrs(retJson)

        val idNrs = getField("id-nr")(retJson)
        //unique
        idNrs.size shouldBe idNrs.toSet.size

        //same count
        countObjects(json) shouldBe idNrs.size
      }
    }
  }
}
