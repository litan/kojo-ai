// #include /nn.kojo
// #include /plot.kojo

val m = 3
val c = 10
val xData0 = Array.tabulate(20)(e => (e + 1).toDouble)
val yData0 = xData0 map (_ * m + c + math.random() * 10 - 5)
val normalizer = new StandardScaler()

val chart = scatterChart("Regression Data", "X", "Y", xData0, yData0)
chart.getStyler.setLegendVisible(true)
drawChart(chart)

val xData = normalizer.fitTransform(xData0)
val yData = yData0

val model = Sequential(Input(-1, 1), Dense(1))

model.compile(mse, tf.train.GradientDescent(0.1f))
model.fit(xData, yData, 20)
val yPreds = model.evaluate(xData)
addLineToChart(chart, Some("model"), xData0, yPreds)
drawChart(chart)
model.close()
