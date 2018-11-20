package com.advancedtelematic.ota.deviceregistry.daemon
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.advancedtelematic.libats.messaging_datatype.MessageCodecs.deviceInstallationReportEncoder
import com.advancedtelematic.libats.test.DatabaseSpec
import com.advancedtelematic.ota.deviceregistry.data.DataType.{DeviceInstallationResult, EcuInstallationResult}
import com.advancedtelematic.ota.deviceregistry.data.InstallationReportGenerators
import com.advancedtelematic.ota.deviceregistry.db.InstallationReportRepository
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, PropSpec}

class DeviceInstallationReportListenerSpec
    extends PropSpec
    with ScalatestRouteTest
    with DatabaseSpec
    with ScalaFutures
    with Matchers
    with InstallationReportGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(Span(10, Seconds), Span(50, Millis))

  val listener = new DeviceInstallationReportListener()

  property("should parse and save DeviceUpdateReport messages") {
    val correlationId = genCorrelationId.sample.get
    val message = genDeviceInstallationReport(correlationId, "0").sample.get
    val deviceUuid = message.device

    listener.apply(message).futureValue shouldBe (())

    val expectedDeviceReports       = Seq(DeviceInstallationResult(correlationId, message.result.code, deviceUuid, message.receivedAt, message.asJson))
    val deviceReports = db.run(InstallationReportRepository.fetchDeviceInstallationReport(correlationId))
    deviceReports.futureValue shouldBe expectedDeviceReports

    val expectedEcuReports = message.ecuReports.map{
      case (ecuId, ecuReport) => EcuInstallationResult(correlationId, ecuReport.result.code, deviceUuid, ecuId)
    }.toSeq
    val ecuReports = db.run(InstallationReportRepository.fetchEcuInstallationReport(correlationId))
    ecuReports.futureValue shouldBe expectedEcuReports

  }

}
