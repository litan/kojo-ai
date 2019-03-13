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
    def apply(input: Double)(implicit mode: Mode): Output[Double]
  }

  case class Sequential(layers: Layer*) {
    var X: Output[Double] = _
    var Y: Output[Double] = _
    var model: Layer0[Output[Double], Output[Double]] = _

    var trainPreds: Output[Double] = _
    var evalPreds: Output[Double] = _
    var inferencePreds: Output[Double] = _

    var trainStep: UntypedOp = _
    var loss: Output[Double] = _
    var graph: Graph = _
    var session: Session = _

    def compile(lossFn: Output[Double] => Output[Double], optimizer: Optimizer) {
      graph = Graph()
      tf.createWith(graph = graph) {
        X = tf.placeholder[Double](Shape(-1, 1), "X")
        Y = tf.placeholder[Double](Shape(-1, 1), "Y")
        model = layers.tail.foldLeft(layers.head.impl)((l1, l2) => l1 >> l2.impl)
        trainPreds = model(X)(tf.learn.TRAINING)
        loss = lossFn(Y - trainPreds)
        trainStep = optimizer.minimize(loss)

        evalPreds = model(X)(tf.learn.EVALUATION)
        inferencePreds = model(X)(tf.learn.INFERENCE)
      }
    }

    def fit(xData: Array[Double], yData: Array[Double], epochs: Int) {
      val xf = toDoubleTensor(xData)
      val yf = toDoubleTensor(yData)

      tf.createWith(graph = graph) {
        session = Session()
        session.run(targets = Seq(tf.globalVariablesInitializer()))
        for (epoch <- 1 to epochs) {
          var feedmap = Map(X -> xf, Y -> yf)
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
        val outs = session.run(fetches = Seq(evalPreds), feeds = Map(X -> xf))
        outs(0).toArray
      }
    }

    def predict(xData: Array[Double]): Array[Double] = {
      val xf = toDoubleTensor(xData)
      tf.createWith(graph = graph) {
        val outs = session.run(fetches = Seq(inferencePreds), feeds = Map(X -> xf))
        outs(0).toArray
      }
    }

    def close(): Unit = {
      session.close()
      graph.close()
    }
  }

  object Counter {
    var counter = 1
    def getAndIncr = {
      val ret = counter
      counter += 1
      ret
    }
  }

  case class Dense(n: Int) extends Layer {
    val impl = tf.learn.Linear[Double](s"Dense${Counter.getAndIncr}", n)
    def apply(input: Double)(implicit mode: Mode) = impl(input)
  }

  case class LeakyRelu(alpha: Double) extends Layer {
    val impl = tf.learn.ReLU[Double](s"Relu${Counter.getAndIncr}", alpha.toFloat)
    def apply(input: Double)(implicit mode: Mode) = impl(input)
  }
}
