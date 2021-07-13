package info.skyblond.teerfeb.backend

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import javax.imageio.ImageIO

object Main {

    private fun initJavalin(): Javalin {
        return Javalin.create { config ->
            config.enableCorsForAllOrigins()
        }.start(7000)
    }


    private fun imgToBase64String(img: BufferedImage): String {
        val os = ByteArrayOutputStream()
        return try {
            ImageIO.write(img, "png", os)
            Base64.getEncoder().encodeToString(os.toByteArray())
        } catch (ioe: IOException) {
            throw UncheckedIOException(ioe)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val app = initJavalin()
        app.routes {
            path("api") {
                post("xray") { ctx ->
                    val imageStream = ctx.uploadedFile("pic") ?: throw BadRequestResponse("No pic uploaded")
                    val bufferedImage = ImageIO.read(imageStream.content)

                    val (result, inputImage) = XrayModel.processPic(bufferedImage)
                    ctx.json(
                        mapOf(
                            "covid" to result[0],
                            "normal" to result[1],
                            "viral" to result[2],
                            "image" to imgToBase64String(inputImage)
                        )
                    )
                }
                
                post("alert") {ctx -> ctx.json(emptyMap<String, Any>())}

                get("view-data") {ctx -> ctx.json(emptyList<Any>())}
            }
        }

    }
}
