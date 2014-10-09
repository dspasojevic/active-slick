/*
 * Copyright 2014 Renato Guerra Cavalcanti (@renatocaval)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.strongtyped.active.slick

trait Tables { this: Profile =>

  import jdbcDriver.simple._

  trait TableWithId[I] {
    def id: Column[I]
  }

  abstract class IdTable[M, I](tag: Tag, schemaName: Option[String], tableName: String)(implicit val colType: BaseColumnType[I])
      extends Table[M](tag, schemaName, tableName) with TableWithId[I] {

    def this(tag: Tag, tableName: String)(implicit mapping: BaseColumnType[I]) = this(tag, None, tableName)
  }

  trait TableWithVersion {
    def version: Column[Long]
  }

  abstract class IdVersionTable[M, I](tag: Tag, schemaName: Option[String], tableName: String)(override implicit val colType: BaseColumnType[I])
      extends IdTable[M, I](tag, schemaName, tableName)(colType) with TableWithVersion {

    def this(tag: Tag, tableName: String)(implicit mapping: BaseColumnType[I]) = this(tag, None, tableName)
  }
}