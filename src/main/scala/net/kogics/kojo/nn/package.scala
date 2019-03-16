package net.kogics.kojo

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import org.platanios.tensorflow.api.Output
import org.platanios.tensorflow.api.UntypedOp
import org.platanios.tensorflow.api.core.Graph
import org.platanios.tensorflow.api.core.Shape
import org.platanios.tensorflow.api.core.client.Session
import org.platanios.tensorflow.api.learn.Mode
import org.platanios.tensorflow.api.learn.layers.{Layer => Layer0}
import org.platanios.tensorflow.api.ops.training.optimizers.Optimizer
import org.platanios.tensorflow.api.tensors.Tensor
import org.platanios.tensorflow.api.tf

package object nn {
  implicit class RichTensor[T: ClassTag](t: Tensor[T]) {
    def toS: String = {
      t.summarize(includeInfo = false)
    }

    def toArray: Array[T] = {
      val buf = ArrayBuffer.empty[T]
      val iter = t.entriesIterator
      while (iter.hasNext) {
        buf += iter.next
      }
      buf.toArray
    }
  }

  def toDoubleTensor(a: Array[Double]) = {
    //        Basic.stack(a.map(e => Tensor.fill(Shape(1))(e)))
    Tensor(a).reshape(Shape(-1, 1))
  }

  val mse = (x: Output[Double]) => tf.mean(tf.square(x))

  trait Layer {
    def impl: Layer0[Output[Double], Output[Double]]
  }

  object Counter {
    var counter = 1
    def getAndIncr = {
      val ret = counter
      counter += 1
      ret
    }
  }

  def shape(dims: Int*) = Shape(dims: _*)

  case class Input(dims: Int*) {
    lazy val placeholder = tf.placeholder[Double](shape(dims: _*), "Input")
  }

  case class Dense(n: Int) extends Layer {
    lazy val impl = tf.learn.Linear[Double](s"Dense${Counter.getAndIncr}", n)
  }

  case class LeakyRelu(alpha: Double) extends Layer {
    lazy val impl = tf.learn.ReLU[Double](s"Relu${Counter.getAndIncr}", alpha.toFloat)
  }

  case object Sigmoid extends Layer {
    lazy val impl = tf.learn.Sigmoid(s"Sigmoid${Counter.getAndIncr}")
  }

  case class Dropout(keep: Double) extends Layer {
    lazy val impl = tf.learn.Dropout[Double](s"Dropout${Counter.getAndIncr}", keep.toFloat)
  }

  case class Sequential(in: Input, layers: Layer*) {
    var input: Output[Double] = _
    var target: Output[Double] = _
    var trainPreds: Output[Double] = _
    var evalPreds: Output[Double] = _
    var inferencePreds: Output[Double] = _

    var trainStep: UntypedOp = _
    var loss: Output[Double] = _
    var graph: Graph = _
    var session: Session = _

    def compile(lossFn: Output[Double] => Output[Double], optimizer: Optimizer) {
      def forward(layers: Seq[Layer], input: Output[Double])(implicit mode: Mode): Output[Double] = {
        var output = input
        layers.foreach { layer =>
          output = layer.impl(output)
        }
        output
      }

      graph = Graph()
      tf.createWith(graph = graph) {
        input = in.placeholder
        trainPreds = forward(layers, input)(tf.learn.TRAINING)
        target = tf.placeholder[Double](trainPreds.shape, "Target")

        loss = lossFn(target - trainPreds)
        trainStep = optimizer.minimize(loss)

        evalPreds = forward(layers, input)(tf.learn.EVALUATION)
        inferencePreds = forward(layers, input)(tf.learn.INFERENCE)
      }
    }

    def fit(xData: Array[Double], yData: Array[Double], epochs: Int) {
      val xf = toDoubleTensor(xData)
      val yf = toDoubleTensor(yData)

      tf.createWith(graph = graph) {
        session = Session()
        session.run(targets = Seq(tf.globalVariablesInitializer()))
        for (epoch <- 1 to epochs) {
          var feedmap = Map(input -> xf, target -> yf)
          session.run(targets = Seq(trainStep), feeds = feedmap)
          val outs = session.run(fetches = Seq(loss), feeds = feedmap)
          val lossO = outs(0)
          println(s"$epoch -- Loss: ${lossO.toS}")
        }
      }
    }

    def evaluate(xData: Array[Double]): Array[Double] = {
      val xf = toDoubleTensor(xData)
      tf.createWith(graph = graph) {
        val outs = session.run(fetches = Seq(evalPreds), feeds = Map(input -> xf))
        outs(0).toArray
      }
    }

    def predict(xData: Array[Double]): Array[Double] = {
      val xf = toDoubleTensor(xData)
      tf.createWith(graph = graph) {
        val outs = session.run(fetches = Seq(inferencePreds), feeds = Map(input -> xf))
        outs(0).toArray
      }
    }

    def close(): Unit = {
      session.close()
      graph.close()
    }

    def describe(): Unit = {
      val graph = Graph()
      tf.createWith(graph = graph) {
        val X = tf.placeholder[Double](Shape(-1, 1), "X")
        var layerInput = X // Tensor.randn(FLOAT64, X.shape)
        println(layerInput.shape)
        layers.foreach { layer =>
          val output = layer.impl(layerInput)(tf.learn.TRAINING)
          println(s"\n* Layer: ${layer.impl.name} -- ${output.shape}")
          val params = graph.trainableVariables.filter(_.name startsWith layer.impl.name)
          println("params:")
          params.foreach { p =>
            println(s"${p.name} - ${p.shape}")
          }
          layerInput = output
        }
      }
      graph.close()
    }
  }
}
