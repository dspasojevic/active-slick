package io.strongtyped.active.slick

import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import io.strongtyped.active.slick.exceptions.StaleObjectStateException
import io.strongtyped.active.slick.test.H2Suite
import org.scalatest._
import slick.dbio.DBIO

class SupplierTest extends FlatSpec with H2Suite with Schema {

  behavior of "A Supplier"

  it should "be persistable" in {
    val initialCount = query(Suppliers.count)

    val supplier = PendingSupplier("Acme, Inc.")

    val savedSupplier =
      commit {
        supplier.save()
      }

    val countAfterSave = query(Suppliers.count)
    countAfterSave shouldBe (initialCount + 1)

    commit(savedSupplier.delete())

    val countAfterDelete = query(Suppliers.count)
    countAfterDelete shouldBe initialCount

  }

  it should "be versionable" in {

    val supplier = PendingSupplier("abc")

    val persistedSupp = commit(supplier.save())
    persistedSupp.version shouldBe 0

    // modify two versions and try to persist them
    val suppWithNewVersion = commit(persistedSupp.copy(name = "abc1").update())
    suppWithNewVersion.version shouldBe 1

    intercept[StaleObjectStateException[Supplier]] {
      // supplier was persisted in the mean time, so version must be different by now
      commit(persistedSupp.copy(name = "abc2").update())
    }

    // supplier with new version can be persisted again
    val suppWithNewerVersion = commit(suppWithNewVersion.copy(name = "abc").update())
    suppWithNewerVersion.version shouldBe 2
  }

  it should "return an error when deleting a supplier with beers linked to it" in {

    val deleteResult =
      rollback {
        for {
          supplier <- PendingSupplier("Acme, Inc.").save()
          beer <- PendingBeer("Abc", supplier.id, 3.2).save()
          deleteResult <- supplier.delete().asTry
        } yield deleteResult
      }

    deleteResult.failure.exception shouldBe a[SQLException]
  }

  override def createSchemaAction: driver.api.DBIO[Unit] = {
    driver.api.DBIO.seq(Suppliers.createSchema, Beers.createSchema)
  }


}
