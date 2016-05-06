package io.strongtyped.active.slick

import io.strongtyped.active.slick.test.H2Suite
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class BeerTest extends FlatSpec with H2Suite with Schema {

  behavior of "A Beer"

  it should "be persistable" in {
    val (supplier, beer) =
      rollback {
        for {
          supplier <- PendingSupplier("Acme, Inc.").save()
          beer <- PendingBeer("Abc", supplier.id, 3.2).save()
          beerSupplier <- beer.supplier()
        } yield {
          beerSupplier.value shouldBe supplier
          (supplier, beer)
        }
      }
  }

  override def createSchemaAction: driver.api.DBIO[Unit] = {
    driver.api.DBIO.seq(Suppliers.createSchema, Beers.createSchema)
  }

}
