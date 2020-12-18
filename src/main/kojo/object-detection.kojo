// copy libk/object_detection.jar to ~/.kojo/lite/libk before running this example

val fps = 5

import java.io.{ BufferedInputStream, File, FileInputStream }
import java.nio.ByteBuffer

import javax.swing.JFrame

import object_detection.protos.StringIntLabelMapOuterClass._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_core.Point
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc.{ COLOR_BGR2RGB, cvtColor, putText, rectangle }
import org.bytedeco.javacv._
import org.platanios.tensorflow.api.{ Graph, Session, Shape, Tensor, UINT8 }
import org.platanios.tensorflow.api.core.client.FeedMap
import org.platanios.tensorflow.proto.GraphDef
import scala.collection.Iterator.continually
import scala.io.Source

import org.platanios.tensorflow.api.UByte
import org.platanios.tensorflow.api.core.types.TFLowestPriority

case class DetectionOutput(boxes: Tensor[Float], scores: Tensor[Float], classes: Tensor[UByte], num: Tensor[UByte])

/**
 * This example shows how to run a pretrained TensorFlow object detection model i.e. one from
 * https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 *
 * You have to download and extract the model you want to run first, like so:
 * $ cd tensorflow
 * $ mkdir models && cd models
 * $ wget http://download.tensorflow.org/models/object_detection/ssd_inception_v2_coco_2017_11_17.tar.gz
 * $ tar xzf ssd_inception_v2_coco_2017_11_17.tar.gz
 *
 * @author Sören Brunk
 */
object ObjectDetector {

    def main(args: Array[String]): Unit = {

        def printUsageAndExit(): Unit = {
            Console.err.println(
                """
          |Usage: ObjectDetector image <file>|video <file>|camera <deviceno> [<modelpath>]
          |  <file>      path to an image/video file
          |  <deviceno>  camera device number (usually starts with 0)
          |  <modelpath> optional path to the object detection model to be used. Default: ssd_inception_v2_coco_2017_11_17
          |""".stripMargin.trim)
            sys.exit(2)
        }

        if (args.length < 2) printUsageAndExit()

        //        val modelDir = args.lift(2).getOrElse("ssd_inception_v2_coco_2017_11_17")
        val modelDir = args.lift(2).getOrElse("ssdlite_mobilenet_v2_coco_2018_05_09")
        // load a pretrained detection model as TensorFlow graph
        val graphDef = GraphDef.parseFrom(
            new BufferedInputStream(new FileInputStream(new File(new File("/home/lalit/work/object-det/models", modelDir), "frozen_inference_graph.pb"))))
        val graph = Graph.fromGraphDef(graphDef)

        // create a session and add our pretrained graph to it
        val session = Session(graph)

        val labelMap: Map[Int, String] = {
            val pbText = new BufferedInputStream(new FileInputStream("/home/lalit/work/object-det/mscoco_label_map.pb"))
            val stringIntLabelMap = StringIntLabelMap.parseFrom(pbText)
            import scala.jdk.CollectionConverters._
            val items = stringIntLabelMap.getItemList.asScala
            var map = Map.empty[Int, String]
            items.foreach { item =>
                map = map.updated(item.getId, item.getDisplayName)
            }
            map
        }

        val inputType = args(0)
        inputType match {
            case "image" =>
                val image = imread(args(1))
                detectImage(image, graph, session, labelMap)
            case "video" =>
                val grabber = new FFmpegFrameGrabber(args(1))
                detectSequence(grabber, graph, session, labelMap)
            case "camera" =>
                val cameraDevice = Integer.parseInt(args(1))
                val grabber = new OpenCVFrameGrabber(cameraDevice)
                detectSequence(grabber, graph, session, labelMap)
            case _ => printUsageAndExit()
        }
    }

    // convert OpenCV tensor to TensorFlow tensor
    def matToTensor(image: Mat): Tensor[UByte] = {
        object xx extends TFLowestPriority
        import xx.uByteEvTF
        val imageRGB = new Mat
        cvtColor(image, imageRGB, COLOR_BGR2RGB) // convert channels from OpenCV GBR to RGB
        val imgBuffer = imageRGB.createBuffer[ByteBuffer]
        val shape = Shape(1, image.size.height, image.size.width(), image.channels)
        Tensor.fromBuffer[UByte](shape, imgBuffer.capacity, imgBuffer)
    }

    // run detector on a single image
    def detectImage(image: Mat, graph: Graph, session: Session, labelMap: Map[Int, String]): Unit = {
        val canvasFrame = new CanvasFrame("Object Detection")
        //        canvasFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE) // exit when the canvas frame is closed
        canvasFrame.setCanvasSize(image.size.width, image.size.height)
        val detectionOutput = detect(matToTensor(image), graph, session)
        drawBoundingBoxes(image, labelMap, detectionOutput)
        canvasFrame.showImage(new OpenCVFrameConverter.ToMat().convert(image))
        canvasFrame.waitKey(0)
        canvasFrame.dispose()
    }

    import java.awt.image.BufferedImage
    def toBufferedImage(mat: Frame): BufferedImage = {
        //        val openCVConverter = new ToMat()
        val java2DConverter = new Java2DFrameConverter()
        java2DConverter.convert(mat)
    }

    // run detector on an image sequence
    var pic: Picture = _
    var lastFrame = epochTimeMillis
    clear()
    def detectSequence(grabber: FrameGrabber, graph: Graph, session: Session, labelMap: Map[Int, String]): Unit = {
        val converter = new OpenCVFrameConverter.ToMat()
        val delay = 1000.0 / fps
        grabber.start()
        try {
            for (
                frame <- continually(grabber.grab()).takeWhile(_ != null)
            ) {
                val currTime = epochTimeMillis
                if (currTime - lastFrame > delay) {
                    val image = converter.convert(frame)
                    if (image != null) { // sometimes the first few frames are empty so we ignore them
                        val detectionOutput = detect(matToTensor(image), graph, session) // run our model
                        drawBoundingBoxes(image, labelMap, detectionOutput)
                        val pic2 = Picture.image(toBufferedImage(frame))
                        if (pic != null) { // show our frame in the preview
                            pic.erase()
                        }
                        pic = pic2
                        pic.draw()
                        lastFrame = currTime
                    }
                }
            }
        }
        finally {
            grabber.stop()
        }

    }

    // run the object detection model on an image
    def detect(image: Tensor[UByte], graph: Graph, session: Session): DetectionOutput = {

        // retrieve the output placeholders
        val imagePlaceholder = graph.getOutputByName("image_tensor:0")
        val detectionBoxes = graph.getOutputByName("detection_boxes:0")
        val detectionScores = graph.getOutputByName("detection_scores:0")
        val detectionClasses = graph.getOutputByName("detection_classes:0")
        val numDetections = graph.getOutputByName("num_detections:0")

        // set image as input parameter
        var feeds = FeedMap.empty
        feeds = feeds + (imagePlaceholder -> image)

        // Run the detection model
        val Seq(boxes, scores, classes, num) =
            session.run(fetches = Seq(detectionBoxes, detectionScores, detectionClasses, numDetections), feeds = feeds)
        DetectionOutput(boxes.asInstanceOf[Tensor[Float]], scores.asInstanceOf[Tensor[Float]],
            classes.asInstanceOf[Tensor[UByte]], num.asInstanceOf[Tensor[UByte]])
    }

    // draw boxes with class and score around detected objects
    def drawBoundingBoxes(image: Mat, labelMap: Map[Int, String], detectionOutput: DetectionOutput): Unit = {
        for (i <- 0 until detectionOutput.boxes.shape.size(1)) {
            val score = detectionOutput.scores(0, i).scalar.asInstanceOf[Float]

            if (score > 0.5) {
                val box = detectionOutput.boxes(0, i).entriesIterator.map(_.asInstanceOf[Float]).toSeq
                // we have to scale the box coordinates to the image size
                val ymin = (box(0) * image.size().height()).toInt
                val xmin = (box(1) * image.size().width()).toInt
                val ymax = (box(2) * image.size().height()).toInt
                val xmax = (box(3) * image.size().width()).toInt
                val label = labelMap.getOrElse(detectionOutput.classes(0, i).scalar.asInstanceOf[Float].toInt, "unknown")

                // draw score value
                putText(
                    image,
                    f"$label%s ($score%1.2f)", // text
                    new Point(xmin + 6, ymin + 38), // text position
                    FONT_HERSHEY_PLAIN, // font type
                    2.6, // font scale
                    new Scalar(0, 0, 0, 0), // text color
                    4, // text thickness
                    LINE_AA, // line type
                    false) // origin is at the top-left corner
                putText(
                    image,
                    f"$label%s ($score%1.2f)", // text
                    new Point(xmin + 4, ymin + 36), // text position
                    FONT_HERSHEY_PLAIN, // font type
                    2.6, // font scale
                    new Scalar(0, 230, 255, 0), // text color
                    4, // text thickness
                    LINE_AA, // line type
                    false) // origin is at the top-left corner
                // draw bounding box
                rectangle(
                    image,
                    new Point(xmin + 1, ymin + 1), // upper left corner
                    new Point(xmax + 1, ymax + 1), // lower right corner
                    new Scalar(0, 0, 0, 0), // color
                    2, // thickness
                    0, // lineType
                    0) // shift
                rectangle(
                    image,
                    new Point(xmin, ymin), // upper left corner
                    new Point(xmax, ymax), // lower right corner
                    new Scalar(0, 230, 255, 0), // color
                    2, // thickness
                    0, // lineType
                    0) // shift
            }
        }
    }
}

ObjectDetector.main(Array("camera", "0"))
// ObjectDetector.main(Array("image", "/home/lalit/Downloads/cells2.jpeg"))
