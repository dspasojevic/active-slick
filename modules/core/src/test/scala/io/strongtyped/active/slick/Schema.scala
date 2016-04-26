package io.strongtyped.active.slick

import io.strongtyped.active.slick.Lens._
import slick.ast.BaseTypedType

import scala.language.existentials

trait Schema extends JdbcProfileProvider {

  case class Supplier(name: String, version: Long = 0, id: Int)

  case class PendingSupplier(name: String)

  case class Beer(name: String,
                  supID: Int,
                  price: Double,
                  id: Int)

  case class PendingBeer(name: String,
                         supID: Int,
                         price: Double)


  class SupplierDao extends EntityActions
  with OptimisticLocking
  with SchemaManagement
  with H2ProfileProvider {

    import jdbcProfile.api._

    val baseTypedType: BaseTypedType[Id] = implicitly[BaseTypedType[Id]]

    type Entity = Supplier
    type PendingEntity = PendingSupplier
    type Id = Int
    type EntityTable = SuppliersTable

    class SuppliersTable(tag: Tag) extends Table[Supplier](tag, "SUPPLIERS") {

      def version = column[Long]("VERSION")

      def name = column[String]("SUPPLIER_NAME")

      def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

      def * = (name, version, id) <>(Supplier.tupled, Supplier.unapply)

    }

    val tableQuery = TableQuery[EntityTable]

    def $id(table: EntityTable) = table.id

    val idLens = lens { supp: Supplier => supp.id } { (supp, id) => supp.copy(id = id) }

    def $version(table: EntityTable) = table.version

    val versionLens = lens { supp: Supplier => supp.version } { (supp, version) => supp.copy(version = version) }

    override def entity(pendingEntity: PendingSupplier): Supplier = Supplier(pendingEntity.name, 0L, -1)
  }

  val Suppliers = new SupplierDao

  implicit class SupplierRecord(val model: Supplier) extends ActiveRecord(Suppliers)

  implicit class PendingSupplierRecord(val pendingModel: PendingSupplier) extends PendingActiveRecord(Suppliers)

  class BeersDao extends EntityActions with SchemaManagement with H2ProfileProvider {

    import jdbcProfile.api._

    val baseTypedType: BaseTypedType[Id] = implicitly[BaseTypedType[Id]]

    type Entity = Beer
    type Id = Int
    type EntityTable = BeersTable

    // Beer Table, DAO and Record extension
    class BeersTable(tag: Tag) extends Table[Beer](tag, "BEERS") {

      def name = column[String]("BEER_NAME")

      def supID = column[Int]("SUP_ID")

      def price = column[Double]("PRICE")

      def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

      def * = (name, supID, price, id) <>(Beer.tupled, Beer.unapply)

      def supplier = foreignKey("SUP_FK", supID, Suppliers.tableQuery)(_.id)
    }

    val tableQuery = TableQuery[EntityTable]

    def $id(table: EntityTable) = table.id

    val idLens = lens { beer: Beer => beer.id } { (beer, id) => beer.copy(id = id) }
    override type PendingEntity = PendingBeer

    override def entity(pendingEntity: PendingEntity): Beer = Beer(pendingEntity.name, pendingEntity.supID, pendingEntity.price, -1)
  }

  val Beers = new BeersDao

  implicit class BeerRecord(val model: Beer) extends ActiveRecord(Beers) {

    def supplier() = Suppliers.findOptionById(model.supID)
  }

  implicit class PendingBeerRecord(val pendingModel: PendingBeer) extends PendingActiveRecord(Beers)

}