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

package object dataframe {
  implicit class DataFrame(df: Table) {
    def length = df.rowCount
    def head(n: Int = 5) = df.first(n)
    def tail(n: Int = 5) = df.last(n)
    def rows(r: Range) = df.rows(r: _*)
    def describe() {
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
