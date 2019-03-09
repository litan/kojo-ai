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

import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler

def drawChart[A <: Styler, B <: Series](chart: Chart[A, B]) {
    import org.knowm.xchart.BitmapEncoder
    cleari()
    val cb = canvasBounds
    val img = BitmapEncoder.getBufferedImage(chart)
    val pic = trans(
        cb.x + (cb.width - img.getWidth) / 2,
        cb.y + (cb.height - img.getHeight) / 2) -> Picture.image(img)
    draw(pic)
}


def plot(t: Table) {
    drawChart(t.makeChart())
}
