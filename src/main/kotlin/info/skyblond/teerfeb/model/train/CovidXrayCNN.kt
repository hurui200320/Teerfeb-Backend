package info.skyblond.teerfeb.model.train

import org.datavec.api.io.labels.ParentPathLabelGenerator
import org.datavec.api.split.FileSplit
import org.datavec.image.loader.NativeImageLoader
import org.datavec.image.recordreader.ImageRecordReader
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
 * DataSet:
 * https://www.kaggle.com/tawsifurrahman/covid19-radiography-database
 * Only Normal, Covid and Viral is used, Lung_Opacity is not used.
 *
 * Dataset structure:
 *   + dataset/covid19_xray
 *   |-+ test
 *   | |-+ covid
 *   | | --+ pictures...
 *   | |-+ normal
 *   | | |-+ pictures...
 *   | |-+ viral
 *   |   |-+ pictures...
 *   |-+ train
 *     |-+ covid
 *     | |-+ pictures...
 *     |-+ normal
 *     | |-+ pictures...
 *     |-+ viral
 *       |-+ pictures...
 *
 * This model should output 3 classes:
 *   0 - Covid
 *   1 - Normal
 *   2 - Viral
 * */
object CovidXrayCNN {
    private val logger = LoggerFactory.getLogger(CovidXrayCNN::class.java)

    const val height = 192
    const val width = 192


    // https://github.com/Hevenicio/Detecting-COVID-19-with-Chest-X-Ray-using-PyTorch
    private const val BASE_PATH = "dataset/covid19_xray"
    private val modelPath = File("models/covid-xray-cnn-model.zip")

    private const val channels: Long = 1
    private const val outputNum = 3

    private const val batchSize = 32
    private const val nEpochs = 300

    private const val seed: Long = 1234
    private val randNumGen = Random(seed)
    private val labelMaker = ParentPathLabelGenerator()

    private fun getNetworkConfig(): MultiLayerConfiguration {
        return NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(0.0005)
            .updater(Adam())
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(
                ConvolutionLayer.Builder(5, 5)
                    .nIn(channels)
                    .stride(1, 1)
                    .nOut(20)
                    .activation(Activation.IDENTITY)
                    .build()
            )
            .layer(
                SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .build()
            )
            .layer(
                ConvolutionLayer.Builder(5, 5)
                    .stride(1, 1) // nIn need not specified in later layers
                    .nOut(50)
                    .activation(Activation.IDENTITY)
                    .build()
            )
            .layer(
                SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .build()
            )
            .layer(
                DenseLayer.Builder().activation(Activation.RELU)
                    .nOut(512)
                    .build()
            )
            .layer(
                DenseLayer.Builder().activation(Activation.RELU)
                    .nOut(128)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nOut(outputNum)
                    .activation(Activation.SOFTMAX)
                    .build()
            )
            .setInputType(
                InputType.convolutionalFlat(
                    height.toLong(),
                    width.toLong(),
                    channels
                )
            ) // InputType.convolutional for normal image
            .build()
    }

    private fun train() {
        logger.info("Data vectorization...")

        val trainData = File("$BASE_PATH/train")
        val trainSplit = FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen)
        val trainRR = ImageRecordReader(height.toLong(), width.toLong(), channels, labelMaker)
        trainRR.initialize(trainSplit)
        val trainIter: DataSetIterator = RecordReaderDataSetIterator(trainRR, batchSize, 1, outputNum)

        logger.info("Labels: {}", trainIter.labels)

        val testData = File("$BASE_PATH/test")
        val testSplit = FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen)
        val testRR = ImageRecordReader(height.toLong(), width.toLong(), channels, labelMaker)
        testRR.initialize(testSplit)
        val testIter: DataSetIterator = RecordReaderDataSetIterator(testRR, batchSize, 1, outputNum)

        val imageScaler: DataNormalization = ImagePreProcessingScaler()
        trainIter.preProcessor = imageScaler
        testIter.preProcessor = imageScaler

        logger.info("Network configuration and training...")

        val conf: MultiLayerConfiguration = getNetworkConfig()
        val model = MultiLayerNetwork(conf)
        model.init()
        logger.info("Summary: {}", model.summary())
        model.setListeners(ScoreIterationListener(10))
        logger.info("Total num of params: {}", model.numParams())

        for (i in 0 until nEpochs) {
            model.fit(trainIter)
            logger.info("Completed epoch {}", i)
            val eval: Evaluation = model.evaluate(testIter)
            logger.info(eval.stats())
            trainIter.reset()
            testIter.reset()
        }

        modelPath.parentFile.mkdirs()
        ModelSerializer.writeModel(model, modelPath, true)
        logger.info("The model has been saved in {}", modelPath.path)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (!modelPath.exists()) {
            train()
        }
    }
}
