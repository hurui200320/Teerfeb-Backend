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
                    get("locations") { ctx ->
                        val continent = ctx.queryParam<String>("continent").getOrNull()?.lowercase()
                        if (continent.isNullOrBlank()) {
                            // return all locations
                            ctx.json(CovidDistributionModel.locationToContinentIndex.keys)
                        } else {
                            // return this continent
                            ctx.json(CovidDistributionModel.continentToLocationIndex[continent] ?: emptyList<String>())
                        }
                    }
                    get("dates") { ctx ->
                        val year = ctx.queryParam<Int>("year").getOrNull()
                        val month = ctx.queryParam<Int>("month").getOrNull()
                        val day = ctx.queryParam<Int>("day").getOrNull()

                        ctx.json(
                            CovidDistributionModel.sortedDateIndex
                                .filter { year == null || it.year == year }
                                .filter { month == null || it.monthValue == month }
                                .filter { day == null || it.dayOfMonth == day }
                        )
                    }
                    get("distribution") { ctx ->
                        val continent = ctx.queryParam<String>("continent").getOrNull()?.lowercase()
                        val location = ctx.queryParam<String>("location").getOrNull()?.lowercase()
                        val year = ctx.queryParam<Int>("year").getOrNull()
                        val month = ctx.queryParam<Int>("month").getOrNull()
                        val day = ctx.queryParam<Int>("day").getOrNull()
                        ctx.json(
                            CovidDistributionModel.data
                                .filter { continent == null || it.continent.lowercase() == continent }
                                .filter { location == null || it.location.lowercase() == location }
                                .filter { year == null || it.date.year == year }
                                .filter { month == null || it.date.monthValue == month }
                                .filter { day == null || it.date.dayOfMonth == day }
                        )
                    }
                }
            }
        }

    }
}
