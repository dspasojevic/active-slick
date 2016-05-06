package io.strongtyped.active.slick

import java.time.{Clock, LocalDateTime}

import slick.ast.BaseTypedType

import scala.concurrent.ExecutionContext

trait MetadataEntityActions[Entity, PendingEntity] extends EntityActions[Entity, PendingEntity] {

  import driver.api._

  def clock: Clock

  def dateCreatedLens: Lens[Entity, LocalDateTime]

  def lastUpdatedLens: Lens[Entity, LocalDateTime]

  override def beforeInsert(entity: Entity)(implicit exc: ExecutionContext): DBIO[Entity] = {
    super.beforeInsert(entity).map { superEntity =>
      dateCreatedLens.set(entity, LocalDateTime.now(clock))
    }
  }

  override def beforeUpdate(id: Id, entity: Entity)(implicit exc: ExecutionContext): DBIO[Entity] = {
    super.beforeUpdate(id, entity).map { superEntity =>
      lastUpdatedLens.set(superEntity, LocalDateTime.now(clock))
    }
  }

}

trait SoftDeleteActions[Entity, PendingEntity] extends EntityActions[Entity, PendingEntity] {

  import driver.api._

  def baseRecordStatusTypedType: BaseTypedType[RecordStatus]

  protected implicit lazy val recordStatusBasedType: BaseTypedType[RecordStatus] = baseRecordStatusTypedType

  type RecordStatus

  def active: RecordStatus

  def deleted: RecordStatus

  def $recordStatus(table: EntityTable): Rep[RecordStatus]

  override def deleteById(id: Id)(implicit exc: ExecutionContext): DBIO[Int] =
    filterById(id).map($recordStatus).update(deleted)

  def activeRecordsFilter(table: EntityTable): Rep[Boolean] = $recordStatus(table) === active

  override def baseQuery: Query[EntityTable, Entity, Seq] = super.baseQuery.filter(activeRecordsFilter)
}
