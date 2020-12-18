// #include /nn.kojo
// #include /plot.kojo

val a = 2
val b = 3
val c = 10
val xData0 = Array.tabulate(20)(e => (e + 1).toDouble)
val yData0 = xData0 map (x => a * x * x + b * x + c + randomDouble(30) - 15)

val xNormalizer = new StandardScaler()
val yNormalizer = new MaxAbsScaler()

val chart = scatterChart("Regression Data and Model", "X", "Y", xData0, yData0)
chart.getStyler.setLegendVisible(true)
drawChart(chart)

val xData = xNormalizer.fitTransform(xData0)
val yData = yNormalizer.fitTransform(yData0)

val wReg = Some(l2(0.005)(_))
val model = Sequential(
    Input(-1, 1),
    Dense(8, wReg), LeakyRelu(0.1),
    Dense(8, wReg), LeakyRelu(0.1),
    Dense(1, wReg))

model.compile(mse, tf.train.Adam())
model.describe()
model.fit(xData, yData, 6000)
val yPreds = model.evaluate(xData)
addLineToChart(chart, Some("model"), xData0, yNormalizer.inverseTransform(yPreds))
drawChart(chart)
model.close()
