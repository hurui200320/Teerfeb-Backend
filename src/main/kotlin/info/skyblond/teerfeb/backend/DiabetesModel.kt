package info.skyblond.teerfeb.backend

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer
import org.nd4j.linalg.factory.Nd4j
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File

object DiabetesModel {
    private const val input = 8

    private val model = getModel()
    private val normalizer = NormalizerSerializer.getDefault()
        .restore<NormalizerStandardize>(File("models/diabetes-mlp-normalizer.zip"))

    private fun getModel(): MultiLayerNetwork {
        return MultiLayerNetwork.load(
            File("models/diabetes-mlp-model.zip"),
            false
        )
    }

    fun processInput(
        pregnancies: Double,
        glucose: Double,
        bloodPressure: Double,
        skinThickness: Double,
        insulin: Double,
        bmi: Double,
        diabetesPedigreeFunction: Double,
        age: Double
    ): Double {
        val input = Nd4j.zeros(1, input)
        input.putScalar(intArrayOf(0, 0), pregnancies)
        input.putScalar(intArrayOf(0, 1), glucose)
        input.putScalar(intArrayOf(0, 2), bloodPressure)
        input.putScalar(intArrayOf(0, 3), skinThickness)
        input.putScalar(intArrayOf(0, 4), insulin)
        input.putScalar(intArrayOf(0, 5), bmi)
        input.putScalar(intArrayOf(0, 6), diabetesPedigreeFunction)
        input.putScalar(intArrayOf(0, 7), age)
        normalizer.transform(input)
        return synchronized(model) {
            val output = model.output(input)
            output.getDouble(0, 0)
        }
    }
}
