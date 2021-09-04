package info.skyblond.teerfeb.backend

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.plugin.json.JavalinJackson
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import javax.imageio.ImageIO


object Main {
    private fun initJavalin(): Javalin {
        JavalinJackson.getObjectMapper()
            .findAndRegisterModules()

        return Javalin.create { config ->
            config.enableCorsForAllOrigins()
        }.start(80)
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
                post("diabetes") { ctx ->
                    val pregnancies = ctx.formParam<Double>("pregnancies").check({ it >= 0 }).get()
                    val glucose = ctx.formParam<Double>("glucose").check({ it > 0 }).get()
                    val bloodPressure = ctx.formParam<Double>("bloodPressure").check({ it > 0 }).get()
                    val skinThickness = ctx.formParam<Double>("skinThickness").check({ it >= 0 }).get()
                    val insulin = ctx.formParam<Double>("insulin").check({ it >= 0 }).get()
                    val bmi = ctx.formParam<Double>("bmi").check({ it > 0 }).get()
                    val function = ctx.formParam<Double>("function").check({ it in 0.0..1.0 }).get()
                    val age = ctx.formParam<Double>("age").check({ it >= 0 }).get()

                    ctx.json(
                        mapOf(
                            "result" to DiabetesModel.processInput(
                                pregnancies, glucose, bloodPressure,
                                skinThickness, insulin,
                                bmi, function, age
                            )
                        )
                    )
                }

                path("covid") {
                    get("continent") { ctx ->
                        ctx.json(CovidDistributionModel.continentToLocationIndex.keys)
                    }
                    get("locations") { ctx ->
                        val continent = ctx.queryParam<String>("continent").getOrNull()?.lowercase()
                        if (continent.isNullOrBlank()) {
                            // return all locations
                            ctx.json(CovidDistributionModel.locationToContinentIndex.keys)
                        } else {
                            // return this continent
                            ctx.json(
                                continent.split(",").flatMap {
                                    CovidDistributionModel.continentToLocationIndex[it] ?: emptyList()
                                }
                            )
                        }
                    }
                    get("date") { ctx ->
                        ctx.json(
                            mapOf(
                                "max" to CovidDistributionModel.sortedDateIndex.last(),
                                "min" to CovidDistributionModel.sortedDateIndex.first(),
                            )
                        )
                    }
                    get("distribution") { ctx ->
                        val location = ctx.queryParam<String>("location").get().lowercase().split(",")
                        val year = ctx.queryParam<Int>("year").get()
                        val month = ctx.queryParam<Int>("month").get()
                        val day = ctx.queryParam<Int>("day").get()
                        ctx.json(
                            CovidDistributionModel.data
                                .filter { it.location.lowercase() in location }
                                .filter { it.date.year == year }
                                .filter { it.date.monthValue == month }
                                .filter { it.date.dayOfMonth == day }
                        )
                    }
                }
            }
        }

    }
}
