package example

import org.knowm.xchart.SwingWrapper
import org.platanios.tensorflow.api.tf

import net.kogics.kojo.preprocess.MaxAbsScaler
import net.kogics.kojo.preprocess.StandardScaler

object NonLinearRegression {
  def main(args: Array[String]): Unit = {
    import net.kogics.kojo.nn._
    import net.kogics.kojo.plot._

    val a = 2
    val b = 3
    val c = 10
    val xData0 = Array.tabulate(20)(e => (e + 1).toDouble)
    val yData0 = xData0 map (x => a * x * x + b * x + c + math.random * 30 - 15)

    val xNormalizer = new StandardScaler()
    val yNormalizer = new MaxAbsScaler()

    val chart = scatterChart("Regression Data", "X", "Y", xData0, yData0)
    chart.getStyler.setLegendVisible(true)
    new SwingWrapper(chart).displayChart()

    val xData = xNormalizer.fitTransform(xData0)
    val yData = yNormalizer.fitTransform(yData0)

    val model = Sequential(
      Dense(64), LeakyRelu(0.1),
      Dense(32), LeakyRelu(0.1),
      Dense(1)
    )

    model.compile(mse, tf.train.Adam())
    model.fit(xData, yData, 3000)
    val yPreds = model.evaluate(xData)
    addLineToChart(chart, Some("model"), xData0, yNormalizer.inverseTransform(yPreds))
    model.close()
  }
}
