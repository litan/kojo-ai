name := "Kojo-AI"

version := "0.1"

scalaVersion := "2.12.8"

fork in run := true

scalacOptions := Seq("-feature", "-deprecation", "-Ypartial-unification")
javaOptions in run ++= Seq("-Xmx1024m", "-Xss1m", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled")

libraryDependencies ++= Seq(
    "org.platanios" %% "tensorflow" % "0.4.1" classifier "linux-cpu-x86_64",
    "org.platanios" %% "tensorflow-data" % "0.4.1",
    "org.knowm.xchart" % "xchart" % "3.5.4",
    "tech.tablesaw" % "tablesaw-core" % "0.31.0"
)

//Build distribution
val distOutpath             = settingKey[File]("Where to copy all dependencies and kojo-ml")
val buildDist  = taskKey[Unit]("Copy runtime dependencies and built kojo-ml to 'distOutpath'")

lazy val dist = project
  .in(file("."))
  .settings(
    distOutpath              := baseDirectory.value / "dist",
    buildDist   := {
      val allLibs:                List[File]          = dependencyClasspath.in(Runtime).value.map(_.data).filter(_.isFile).toList
      val buildArtifact:          File                = packageBin.in(Runtime).value
      val jars:                   List[File]          = buildArtifact :: allLibs
      val `mappings src->dest`:   List[(File, File)]  = jars.map(f => (f, distOutpath.value / f.getName))
      val log                                         = streams.value.log
      log.info(s"Copying to ${distOutpath.value}:")
      log.info(s"${`mappings src->dest`.map(f => s" * ${f._1}").mkString("\n")}")
      IO.copy(`mappings src->dest`)
    }
  )

