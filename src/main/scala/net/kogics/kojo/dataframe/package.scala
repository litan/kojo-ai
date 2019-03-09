package net.kogics.kojo

import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf

import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler

import tech.tablesaw.aggregate.AggregateFunctions.max
import tech.tablesaw.aggregate.AggregateFunctions.mean
import tech.tablesaw.aggregate.AggregateFunctions.median
import tech.tablesaw.aggregate.AggregateFunctions.min
import tech.tablesaw.aggregate.AggregateFunctions.quartile1
import tech.tablesaw.aggregate.AggregateFunctions.quartile3
import tech.tablesaw.aggregate.AggregateFunctions.standardDeviation
import tech.tablesaw.api.IntColumn
import tech.tablesaw.api.NumericColumn
import tech.tablesaw.api.Row
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.columns.Column
import tech.tablesaw.io.csv.CsvReadOptions

package object dataframe {
  def readCsv(filename: String, separator: Char = ',', header: Boolean = true): Table = {
    val optionsBuilder =
      CsvReadOptions.builder(filename)
        .separator(separator) // table is tab-delimited
        .header(header)
    Table.read().csv(optionsBuilder.build())
  }

  def writeCsv(table: Table, filename: String): Unit = {
    table.write().csv(filename)
  }

  implicit class DataFrame(df: Table) {
    def length: Int = df.rowCount
    def head(n: Int = 5): Table = df.first(n)
    def tail(n: Int = 5): Table = df.last(n)
    def rows(n: Seq[Int]): Table = df.rows(n: _*)
    def columns[T: TypeTag](xs: Seq[T]): Table = {
      import scala.collection.JavaConverters._
      typeOf[T] match {
        case t if t =:= typeOf[Int] =>
          Table.create(df.name, df.columns(xs.asInstanceOf[Seq[Int]]: _*).asScala: _*)
        case t if t =:= typeOf[String] =>
          Table.create(df.name, df.columns(xs.asInstanceOf[Seq[String]]: _*).asScala: _*)
        case _ =>
          throw new RuntimeException("Invalid column index")
      }
    }
    def rowCols[T: TypeTag](rs: Seq[Int], cs: Seq[T]): Table = {
      columns(cs).rows(rs)
    }
    def describe(): Unit = {
      for (idx <- 0 until df.columnCount) {
        val column = df.column(idx)
        println("===")
        column match {
          case nc: NumericColumn[_] =>
            println(s"Column: ${column.name}")
            println(s"mean: ${mean.summarize(nc)}")
            println(s"std: ${standardDeviation.summarize(nc)}")
            println(s"min: ${min.summarize(nc)}")
            println(s"25%: ${quartile1.summarize(nc)}")
            println(s"50%: ${median.summarize(nc)}")
            println(s"75%: ${quartile3.summarize(nc)}")
            println(s"max: ${max.summarize(nc)}")

          case sc: StringColumn =>
            println(df.categoricalColumn(idx).countByCategory)

          case _ =>
        }
      }
    }

    def makeChart[A <: Styler, B <: Series](): Chart[A, B] = {
      import net.kogics.kojo.plot._
      val cnt = df.columnCount
      require(cnt == 1 || cnt == 2, "Frame should have one or two columns")
      if (cnt == 1) {
        val column = df.column(0)
        column match {
          case nc: NumericColumn[_] =>
            histogram(nc.name, nc.name, "Count", nc.asDoubleArray(), 20).asInstanceOf[Chart[A, B]]
          case sc: StringColumn =>
            val cc = df.categoricalColumn(0)
            val catcnt = cc.countByCategory
            barChart(cc.name, cc.name, "Counts", catcnt.stringColumn(0).asObjectArray,
              catcnt.intColumn(1).asObjectArray().map(_.toInt)).asInstanceOf[Chart[A, B]]
          case _ => null
        }
      }
      else {
        null
      }
    }
  }

  trait ColumnAdder[A] {
    def addColumn(df: Table, name: String)(filler: Row => A): Column[A]
  }

  object ColumnAdderInstances {

    def initColumn[A](df: Table, col: Column[A])(filler: Row => A): Column[A] = {
      df.forEach(row => col.appendMissing())
      df.forEach(row => col.set(row.getRowNumber, filler(row)))
      df.addColumns(col)
      col
    }

    implicit val stringColumn: ColumnAdder[String] =
      new ColumnAdder[String] {
        def addColumn(df: Table, name: String)(filler: Row => String): Column[String] = {
          val col = StringColumn.create(name)
          initColumn(df, col)(filler)
        }
      }

    implicit val intColumn: ColumnAdder[Int] =
      new ColumnAdder[Int] {
        def addColumn(df: Table, name: String)(filler: Row => Int): Column[Int] = {
          val col = IntColumn.create(name)
          initColumn[Int](df, col.asInstanceOf[Column[Int]])(filler)
        }
      }
  }

  object ColumnAdderSyntax {
    implicit class ColumnAdderOps[A](df: Table) {
      def addColumn(name: String)(filler: Row => A)(implicit ca: ColumnAdder[A]): Column[A] = {
        ca.addColumn(df, name)(filler)
      }
    }
  }
}
