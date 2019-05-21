// #include /dataframe.kojo

// Load a couple of dataframes from csv files in the kojo-ai data dir
val marks = readCsv("/home/lalit/work/kojo-ai/data/student-marks.csv")
val info = readCsv("/home/lalit/work/kojo-ai/data/student-info.csv")

// Do an inner join on the frames based on the "Name" field
val marksInfo = marks.join("Name").inner(info)

// Find mean math marks grouped by area
marksInfo.summarize("Math", mean).by("Area")

// Do a selection - find all students with math marks greater than 90
marksInfo.where(marksInfo.intColumn("Math").isGreaterThan(90))

// draw a histogram (with 7 bins) of math marks
drawChart(marksInfo.columns(Seq("Math")).makeHistogram(7))

// draw a bar chart of the areas where the students live
drawChart(marksInfo.columns(Seq("Area")).makeBarChart())