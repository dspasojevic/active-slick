package io.strongtyped.active.slick

import io.strongtyped.active.slick.exceptions.{NoRowsAffectedException, StaleObjectStateException}
import io.strongtyped.active.slick.DBIOExtensions._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait OptimisticLocking {
  self: EntityActions =>


  import self.jdbcProfile.api._

  def $version(table: EntityTable): Rep[Long]

  def versionLens: Lens[self.Entity, Long]

  override protected def update(id: self.Id, versionable: self.Entity)(implicit exc: ExecutionContext): DBIO[self.Entity] = {

    // extract current version
    val currentVersion = versionLens.get(versionable)

    // build a query selecting entity with current version
    val queryByIdAndVersion = self.filterById(id).filter($version(_) === currentVersion)

    // model with incremented version
    val modelWithNewVersion = versionLens.set(versionable, currentVersion + 1)

    val tryUpdate = queryByIdAndVersion.update(modelWithNewVersion).mustAffectOneSingleRow.asTry

    // in case of failure, we want a more meaningful exception ie: StaleObjectStateException
    tryUpdate.flatMap {
      case Success(_)                       => DBIO.successful(modelWithNewVersion)
      case Failure(NoRowsAffectedException) => DBIO.failed(new StaleObjectStateException(versionable))
      case Failure(e)                       => DBIO.failed(e)
    }
  }

  override def update(versionable: self.Entity)(implicit exc: ExecutionContext): DBIO[self.Entity] = {
    val id = self.idLens.get(versionable)
    update(id, versionable)
  }


}