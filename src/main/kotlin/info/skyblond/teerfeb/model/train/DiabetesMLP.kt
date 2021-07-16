package info.skyblond.teerfeb.model.train

import com.opencsv.CSVReader
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.evaluation.classification.ROC
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * https://www.kaggle.com/uciml/pima-indians-diabetes-database
 * */
object DiabetesMLP {
    private val logger = LoggerFactory.getLogger(DiabetesMLP::class.java)
    private const val BASE_PATH = "dataset"
    private val modelPath = File("models/diabetes-mlp-model.zip")
    private val normalizerPath = File("models/diabetes-mlp-normalizer.zip")

    private const val seed: Long = 1234

    private fun getNetworkConfig(): MultiLayerConfiguration {
        return NeuralNetConfiguration.Builder()
            .seed(seed)
            .updater(Nesterovs())
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(
                DenseLayer.Builder()
                    .nIn(8)
                    .nOut(5)
                    .weightInit(WeightInit.XAVIER)
                    .activation(Activation.ELU)
                    .build()
            )
            .layer(
                OutputLayer.Builder()
                    .nOut(1)
                    .weightInit(WeightInit.XAVIER)
                    .activation(Activation.SIGMOID)
                    .lossFunction(LossFunctions.LossFunction.XENT)
                    .build()
            )
            .build()
    }

    private fun getDataSet(data: List<Pair<DoubleArray, Double>>, batchSize: Int): INDArrayDataSetIterator {
        return INDArrayDataSetIterator(
            data.map { pair ->
                org.nd4j.common.primitives.Pair(
                    Nd4j.create(pair.first),
                    Nd4j.create(doubleArrayOf(pair.second))
                )
            },
            batchSize
        )
    }

    private fun train() {
        val lines = File("$BASE_PATH/diabetes.csv").reader(StandardCharsets.UTF_8)
            .use { reader ->
                CSVReader(reader).use {
                    it.readAll()
                }
            }
        val data = lines.drop(1)
            .map { sample -> sample.map { it.toDouble() } }
            // last one is output
            .map { it.dropLast(1) to it.last() }
            .map { it.first.toDoubleArray() to it.second }
            .shuffled()
        println("Total samples: " + data.size)

        val takeRate = 0.8
        val trainDatasetIter = getDataSet(
            data.take((data.size * takeRate).toInt()), 64
        )
        val testDatasetIter = getDataSet(
            data.drop((data.size * takeRate).toInt()), 1
        )

        val normalizer = NormalizerStandardize()
        normalizer.fit(trainDatasetIter)
        NormalizerSerializer.getDefault().write(normalizer, normalizerPath)
        trainDatasetIter.preProcessor = normalizer
        testDatasetIter.preProcessor = normalizer

        val model = MultiLayerNetwork(getNetworkConfig())
        model.init()
        logger.info(model.summary())
        model.setListeners(ScoreIterationListener(50))

        model.fit(trainDatasetIter, 500)
        model.save(modelPath, true)

        // evaluate the model on the test set
        val eval = model.evaluate<Evaluation>(testDatasetIter)
        val roc = model.evaluateROC<ROC>(testDatasetIter, 0)

        logger.info("Stats: {}", eval.stats())
        logger.info("Roc stats: \n{}", roc.stats())
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (!modelPath.exists()) {
            train()
        }
    }
}
