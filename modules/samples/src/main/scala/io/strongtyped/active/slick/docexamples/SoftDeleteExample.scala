package io.strongtyped.active.slick.docexamples

import io.strongtyped.active.slick.Lens._
import io.strongtyped.active.slick._
import slick.ast.BaseTypedType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object SoftDeleteExample {

  //@formatter:off
  // tag::adoc[]
  case class Coffee(name: String, version: Long = 0, id: Int)

  case class PendingCoffee(name: String)

  object CoffeeRepo extends EntityActions with SoftDeleteActions with H2ProfileProvider {

    import jdbcProfile.api._

    def $version(table: CoffeeTable): Rep[Long] = table.version // #<1>
    def versionLens = lens { coffee:Coffee => coffee.version }  // #<2>
                           { (coffee, vers) => coffee.copy(version = vers) }


    class CoffeeTable(tag: Tag) extends Table[Coffee](tag, "COFFEE") {
      def name = column[String]("NAME")
      def id = column[Id]("ID", O.PrimaryKey, O.AutoInc)
      def version = column[Long]("VERSION")
      def status = column[Long]("STATUS")
      def * = (name, version, id) <>(Coffee.tupled, Coffee.unapply)
    }
    // end::adoc[]
    //@formatter:on

    val baseTypedType = implicitly[BaseTypedType[Id]]
    override def baseRecordStatusTypedType: BaseTypedType[RecordStatus] = implicitly[BaseTypedType[RecordStatus]]


    type PendingEntity = PendingCoffee
    type Entity = Coffee
    type Id = Int
    type EntityTable = CoffeeTable

    override val tableQuery = TableQuery[CoffeeTable]

    def $id(table: CoffeeTable): Rep[Id] = table.id

    val idLens = lens { coffee: Coffee => coffee.id } { (coffee, id) => coffee.copy(id = id) }


    def findByName(name: String): DBIO[Seq[Coffee]] = {
      tableQuery.filter(_.name === name).result
    }

    override def entity(pendingEntity: PendingCoffee): Coffee = Coffee(pendingEntity.name, 0, -1)

    override val deleted = 0L

    override val active = 1L

    override def $recordStatus(table: CoffeeTable): Rep[RecordStatus] = table.status

    override type RecordStatus = Long
  }

  implicit class EntryExtensions(val model: Coffee) extends ActiveRecord(CoffeeRepo)

  implicit class PendingEntryExtensions(val pendingModel: PendingCoffee) extends PendingActiveRecord(CoffeeRepo)

  val saveAction = PendingCoffee("Colombia").save()

}
