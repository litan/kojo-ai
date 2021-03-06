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
    val yData0 = xData0 map (x => a * x * x + b * x + c + math.random() * 40 - 20)

    val xNormalizer = new StandardScaler()
    val yNormalizer = new MaxAbsScaler()

    val chart = scatterChart("Regression Data", "X", "Y", xData0, yData0)
    chart.getStyler.setLegendVisible(true)
    val chartWin = new SwingWrapper(chart).displayChart()

    val xData = xNormalizer.fitTransform(xData0)
    val yData = yNormalizer.fitTransform(yData0)

    val wReg = Some(l2(0.005)(_))
    val model = Sequential(
      Input(-1, 1),
      Dense(8, wReg), LeakyRelu(0.1),
      Dense(8, wReg), LeakyRelu(0.1),
      Dense(1, wReg)
    )

    model.compile(mse, tf.train.Adam())
    model.describe()
    model.fit(xData, yData, 6000)
    val yPreds = model.evaluate(xData)
    addLineToChart(chart, Some("model"), xData0, yNormalizer.inverseTransform(yPreds))
    chartWin.repaint()
    model.close()
  }
}
