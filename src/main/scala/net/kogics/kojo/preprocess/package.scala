package net.kogics.kojo

import org.apache.commons.math3.stat.StatUtils

package object preprocess {
  trait Scaler {
    def fit(data: Array[Double]): Unit
    def transform(data: Array[Double]): Array[Double]
    def inverseTransform(data: Array[Double]): Array[Double]
    def fitTransform(data: Array[Double]): Array[Double] = {
      fit(data)
      transform(data)
    }
  }
  class StandardScaler extends Scaler {
    var mean = 0.0
    var std = 0.0
    def fit(data: Array[Double]) = {
      mean = StatUtils.mean(data)
      std = math.sqrt(StatUtils.variance(data, mean))
      println(mean, std)
    }

    def transform(data: Array[Double]) = {
      data map (e => (e - mean) / std)
    }

    def inverseTransform(data: Array[Double]) = {
      data map (e => e * std + mean)
    }
  }

  class MaxAbsScaler extends Scaler {
    var absMax = 0.0
    def fit(data: Array[Double]): Unit = {
      absMax = data.max.abs
    }
    def transform(data: Array[Double]): Array[Double] = {
      data map (_ / absMax)
    }

    def inverseTransform(data: Array[Double]): Array[Double] = {
      data map (_ * absMax)
    }
  }
}
