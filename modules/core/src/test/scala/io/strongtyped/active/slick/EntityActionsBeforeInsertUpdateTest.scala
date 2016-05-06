package io.strongtyped.active.slick

import io.strongtyped.active.slick.JdbcProfileProvider.H2ProfileProvider
import io.strongtyped.active.slick.test.H2Suite
import org.scalatest.FlatSpec
import slick.ast.BaseTypedType
import io.strongtyped.active.slick.Lens._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


class EntityActionsBeforeInsertUpdateTest
  extends FlatSpec with H2Suite with JdbcProfileProvider {

  behavior of "An EntityDao with validation "

  it should "return an error if beforeInsert is not successful" in {

    val result =
      rollback {
        for {
          result <- PendingFoo("  ").save().asTry
        } yield result
      }

    result.failure.exception shouldBe a[NameShouldNotBeEmptyException]
  }

  it should "return an error if beforeUpdate is not successful" in {

    val result =
      rollback {

        val finalAction =
          for {
            savedEntry <- PendingFoo("abc").save()
            // update name should fail according to beforeUpdate method definition
            updatedEntry <- savedEntry.copy(name = "Bar").update()
          } yield updatedEntry

        finalAction.asTry
      }

    result.failure.exception shouldBe a[NameCanNotBeModifiedException]
  }


  override def createSchemaAction = {
    Foos.createSchema
  }


  case class Foo(name: String, id: Int)

  case class PendingFoo(name: String)

  class NameShouldNotBeEmptyException extends RuntimeException("Name should not be empty")

  class NameCanNotBeModifiedException extends RuntimeException("Name can not be modified")

  class FooDao extends EntityActions[Foo, PendingFoo] with H2ProfileProvider {

    import driver.api._

    val baseTypedType: BaseTypedType[Id] = implicitly[BaseTypedType[Id]]

    type EntityTable = FooTable
    type PendingEntity = PendingFoo
    type Entity = Foo
    type Id = Int

    class FooTable(tag: Tag) extends Table[Foo](tag, "FOO_VALIDATION_TEST") {

      def name = column[String]("NAME")

      def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

      def * = (name, id) <>(Foo.tupled, Foo.unapply)

    }

    val tableQuery = TableQuery[FooTable]

    def $id(table: FooTable) = table.id

    val idLens = lens { foo: Foo => foo.id } { (entry, id) => entry.copy(id = id) }

    //@formatter:off
    // tag::adoc[]
    override def beforeInsert(model: Foo)
                             (implicit exc: ExecutionContext): DBIO[Foo] = {
      if (model.name.trim.isEmpty) {
        DBIO.failed(new NameShouldNotBeEmptyException)
      } else {
        DBIO.successful(model)
      }
    }

    override def beforeUpdate(id: Int, model: Foo)
                             (implicit exc: ExecutionContext): DBIO[Foo] = {
      findById(id).flatMap { oldModel =>
        if (oldModel.name != model.name) {
          DBIO.failed(new NameCanNotBeModifiedException)
        } else {
          DBIO.successful(model)
        }
      }
    }
    // end::adoc[]
    //@formatter:on

    def createSchema = {
      import driver.api._
      tableQuery.schema.create
    }

    override def entity(pendingEntity: PendingFoo): Foo = Foo(pendingEntity.name, -1)
  }

  val Foos = new FooDao

  implicit class EntryExtensions(val model: Foo) extends ActiveRecord[Foo, FooDao](Foos)

  implicit class PendingEntryExtensions(val pendingModel: PendingFoo) extends PendingActiveRecord[Foo, PendingFoo, FooDao](Foos)

}
