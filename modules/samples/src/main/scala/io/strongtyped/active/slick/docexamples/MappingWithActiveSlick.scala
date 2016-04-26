package io.strongtyped.active.slick.docexamples

//@formatter:off
// tag::adoc[]
import io.strongtyped.active.slick._
import slick.ast.BaseTypedType
import slick.driver.H2Driver
import io.strongtyped.active.slick.Lens._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object MappingWithActiveSlick {

  case class Coffee(name: String, id: Int)

  case class PendingCoffee(name: String)

  object CoffeeRepo extends EntityActions with H2ProfileProvider {

    import jdbcProfile.api._ // #<1>
    val baseTypedType = implicitly[BaseTypedType[Id]] // #<2>

    type PendingEntity = PendingCoffee
    type Entity = Coffee // #<3>
    type Id = Int // #<4>
    type EntityTable = CoffeeTable // # <5>

    val tableQuery = TableQuery[CoffeeTable] // # <6>

    def $id(table: CoffeeTable): Rep[Id] = table.id // # <7>
    
    val idLens = lens { coffee: Coffee => coffee.id  } // # <8>
                      { (coffee, id) => coffee.copy(id = id) } 

    override def entity(pendingEntity: PendingCoffee): Coffee = Coffee(pendingEntity.name, -1)

    class CoffeeTable(tag: Tag) extends Table[Coffee](tag, "COFFEE") { // #<9>
      def name = column[String]("NAME")
      def id = column[Id]("ID", O.PrimaryKey, O.AutoInc)
      def * = (name, id) <>(Coffee.tupled, Coffee.unapply)
    }

    def findByName(name:String): DBIO[Seq[Coffee]] = {
      tableQuery.filter(_.name === name).result
    }

}

  implicit class EntryExtensions(val model: Coffee) extends ActiveRecord(CoffeeRepo)

  implicit class PendingEntryExtension(val pendingModel: PendingCoffee) extends PendingActiveRecord(CoffeeRepo)

  val saveAction = PendingCoffee("Colombia").save()
}
// end::adoc[]
//@formatter:on
