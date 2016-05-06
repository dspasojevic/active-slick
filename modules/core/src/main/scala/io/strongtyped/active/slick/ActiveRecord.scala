package io.strongtyped.active.slick

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext


abstract class ActiveRecord[Model, R <: CrudActions[Model, _]](val repository: R) {

  def model: Model

  def update()(implicit exc: ExecutionContext): DBIO[Model] =
    repository.update(model)

  def delete()(implicit exc: ExecutionContext): DBIO[Int] = repository.delete(model)

}

abstract class PendingActiveRecord[Model, PendingModel, R <: CrudActions[Model, PendingModel]](val repository: R) {

  def pendingModel: PendingModel

  def save()(implicit exc: ExecutionContext): DBIO[Model] =
    repository.create(pendingModel)

}


