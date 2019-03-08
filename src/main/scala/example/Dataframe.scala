package example

import dataframe.ColumnAdderInstances._
import dataframe.ColumnAdderSyntax._
import dataframe._
import plot._
import tech.tablesaw.aggregate.AggregateFunctions.sum
import tech.tablesaw.api.Table

object Dataframe {
  def main(args: Array[String]): Unit = {
    val df = Table.read().csv("/home/lalit/t1/Downloads/xAPI-Edu-Data.csv")
    df.structure
    df.head()
    df.stringColumn("gender").isMissing().size

    val cats = df.categoricalColumn("Topic").countByCategory.stringColumn("Category").asObjectArray
    val counts = df.categoricalColumn("Topic").countByCategory.intColumn("Count").asObjectArray.map(_.toInt)
    val chart = barChart("Subject Counts", "Subject", "Count", cats, counts)
    //    new SwingWrapper(chart).displayChart()

    df.addColumn("Failed") { row => if (row.getString("Class") == "L") 1 else 0 }
    df.select("Class", "Failed")

    // group-by; split-apply-combine
    println(df.summarize("Failed", sum).by("Topic"))
    // cross tab
    df.xTabCounts("Topic", "Class")
    // frequencies for categorical var
    df.categoricalColumn("Class").countByCategory
  }
}
