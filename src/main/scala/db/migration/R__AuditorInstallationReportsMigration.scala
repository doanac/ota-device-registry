package db.migration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.advancedtelematic.libats.http.ServiceHttpClientSupport
import com.advancedtelematic.libats.slick.db.AppMigration
import com.advancedtelematic.ota.deviceregistry.client.AuditorHttpClient
import com.advancedtelematic.ota.deviceregistry.db.MigrateOldInstallationReports
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile

import scala.concurrent.Future

class R__AuditorInstallationReportsMigration extends AppMigration with ServiceHttpClientSupport {

  implicit val system: ActorSystem = ActorSystem(this.getClass.getSimpleName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  private lazy val _log = LoggerFactory.getLogger(this.getClass)

  private val auditorUri = ConfigFactory.load().getString("auditor.uri")
  private val auditor = new AuditorHttpClient(auditorUri, defaultHttpClient)

  override def migrate(implicit db: MySQLProfile.api.Database): Future[Unit] =
    if (auditorUri.length() > 0 ) {
      new MigrateOldInstallationReports(auditor).run.map(_ => ())
    } else {
      _log.warn("Auditor API isn't enabled, skipping migration")
    }
}
