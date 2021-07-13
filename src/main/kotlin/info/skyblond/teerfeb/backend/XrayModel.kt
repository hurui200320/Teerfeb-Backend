package info.skyblond.teerfeb.backend

import info.skyblond.teerfeb.model.train.CovidXrayCNN
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler
import org.nd4j.linalg.factory.Nd4j
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File

object XrayModel {
    private const val height = CovidXrayCNN.height
    private const val width = CovidXrayCNN.width

    private val model = getModel()
    private val normalizer = ImagePreProcessingScaler()

    private fun getModel(): MultiLayerNetwork {
        return MultiLayerNetwork.load(
            File("models/covid-xray-cnn-model.zip"),
            false
        )
    }

    fun processPic(image: BufferedImage): Pair<DoubleArray, BufferedImage> {
        val bufferedImage = resizeImage(picToGrayScale(image))
        val raster = bufferedImage.raster
        // [batch, channel, height, width]
        val input = Nd4j.zeros(1, 1, height, width)
        for (x in 0 until width) {
            for (y in 0 until height) {
                input.putScalar(
                    intArrayOf(0, 0, y, x),
                    raster.getPixel(x, y, null as DoubleArray?)[0]
                )
            }
        }
        normalizer.transform(input)
        return synchronized(model) {
            val output = model.output(input)
            //covid, normal, viral
            DoubleArray(3) { output.getDouble(0, it) }
        } to bufferedImage
    }

    private fun picToGrayScale(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_BYTE_GRAY)
            return image

        val bufferedImage = BufferedImage(
            image.width, image.height,
            BufferedImage.TYPE_BYTE_GRAY
        )

        val newImageRaster = bufferedImage.raster

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getRGB(x, y))
                val bright = color.red * 0.299 + color.green * 0.587 + color.blue * 0.114
                newImageRaster.setPixel(
                    x, y,
                    doubleArrayOf(bright)
                )
            }
        }

        return bufferedImage
    }

    private fun resizeImage(img: BufferedImage): BufferedImage {
        val tmp: Image = img.getScaledInstance(width, height, Image.SCALE_FAST)
        val newImg = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val g2d = newImg.createGraphics()
        g2d.drawImage(tmp, 0, 0, null)
        g2d.dispose()
        return newImg
    }
}
