// #include /plot.kojo

import net.kogics.kojo.dataframe._
import ColumnAdderInstances._
import ColumnAdderSyntax._
import tech.tablesaw.aggregate.AggregateFunctions.sum
import tech.tablesaw.aggregate.AggregateFunctions.max
import tech.tablesaw.aggregate.AggregateFunctions.mean
import tech.tablesaw.aggregate.AggregateFunctions.median
import tech.tablesaw.aggregate.AggregateFunctions.min
import tech.tablesaw.aggregate.AggregateFunctions.quartile1
import tech.tablesaw.aggregate.AggregateFunctions.quartile3
import tech.tablesaw.aggregate.AggregateFunctions.standardDeviation
import tech.tablesaw.api.Table

def barChart(t: Table) {
    drawChart(t.makeBarChart())
}

def pieChart(t: Table) {
    drawChart(t.makePieChart())
}

def histogram(t: Table, bins: Int = 10) {
    drawChart(t.makeHistogram(bins))
}

def lineChart(t: Table) {
    drawChart(t.makeLineChart())
}

def scatterChart(t: Table) {
    drawChart(t.makeScatterChart())
}
