package watermark

import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Transparency
import kotlin.system.exitProcess
import java.awt.Point

fun main() {
    val image = imageParamsInput()
    val watermarkImage = watermarkParamsInput()

    checkImagesSize(image, watermarkImage)
    var (useAlphaChannel, color) = alphaSettings(watermarkImage)
    val transpPercent = transpPercentInput()
    val offsetPoint = getPositionMethod(image, watermarkImage)


    println("Input the output image filename (jpg or png extension):")
    var outputFileName = readln()
    var outputFile =File(outputFileName)
    if (outputFile.extension != "png" && outputFile.extension != "jpg") {
        println("The output file extension isn't \"jpg\" or \"png\".")
    }

    val newImage = applyWatermark(image, watermarkImage, transpPercent, useAlphaChannel, color, offsetPoint)
    ImageIO.write(newImage, outputFile.extension, outputFile)
    println("The watermarked image $outputFileName has been created.")
}

fun applyWatermark(image: BufferedImage, watermark: BufferedImage, weight: Int,
                   useAlphaChannel: Boolean, alphaColor: Color?, offsetPoint: Point) : BufferedImage {
    val newImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until image.height){
        for (x in 0 until image.width) {
            var watermarkX: Int
            var watermarkY: Int

            if (offsetPoint.x == -1 && offsetPoint.y == -1) {
                watermarkX = x % watermark.width
                watermarkY = y % watermark.height
            }
            else {
                watermarkX = if (x - offsetPoint.x < 0 || x - offsetPoint.x > watermark.width - 1) 0 else x - offsetPoint.x
                watermarkY = if (y - offsetPoint.y < 0 || y - offsetPoint.y > watermark.height - 1) 0 else y - offsetPoint.y
            }

            val watermarkColor = Color(watermark.getRGB(watermarkX, watermarkY), useAlphaChannel)
            val imageColor = Color(image.getRGB(x, y))

            if (watermarkColor.alpha == 0 || alphaColor != null && alphaColor == watermarkColor
                || offsetPoint.x > x || offsetPoint.y > y || x - offsetPoint.x > watermark.width - 1 && offsetPoint.x != -1 ||
                y - offsetPoint.y > watermark.height - 1 && offsetPoint.x != -1){
                newImage.setRGB(x, y, imageColor.rgb)
                continue
            }
            newImage.setRGB(x, y, getNewColor(imageColor, watermarkColor, weight).rgb)
        }
    }
    return newImage
}

fun getNewColor(imageColor: Color, watermarkColor: Color, weight: Int) : Color{
    val newRed = ((100 - weight) * imageColor.red + weight * watermarkColor.red) / 100
    val newGreen = ((100 - weight) * imageColor.green + weight * watermarkColor.green) / 100
    val newBlue = ((100 - weight) * imageColor.blue + weight * watermarkColor.blue) / 100
    return Color(newRed, newGreen, newBlue)
}

fun imageParamsInput() : BufferedImage {
    println("Input the image filename:")
    var fileName = readln()
    var file = File(fileName)

    if (!file.exists()) {
        println("The file ${fileName} doesn't exist.")
        exitProcess(-1)
    }
    var image = ImageIO.read(file)

    if (image.colorModel.numComponents != 3) {
        println("The number of image color components isn't 3.")
        exitProcess(-1)
    }

    val pixelSize = image.colorModel.pixelSize
    if (pixelSize != 24 && pixelSize != 32) {
        println("The image isn't 24 or 32-bit.")
        exitProcess(-1)
    }
    return image
}

fun watermarkParamsInput() : BufferedImage {
    println("Input the watermark image filename:")
    var watermarkFileName = readln()
    var watermarkFile = File(watermarkFileName)

    if (!watermarkFile.exists()) {
        println("The file ${watermarkFileName} doesn't exist.")
        exitProcess(-1)
    }
    var watermarkImage = ImageIO.read(watermarkFile)

    if (watermarkImage.colorModel.numComponents != 3 && watermarkImage.transparency != Transparency.TRANSLUCENT) {
        println("The number of watermark color components isn't 3.")
        exitProcess(-1)
    }

    val watermarkPixelSize = watermarkImage.colorModel.pixelSize
    if (watermarkPixelSize != 24 && watermarkPixelSize != 32) {
        println("The watermark isn't 24 or 32-bit.")
        exitProcess(-1)
    }
    return watermarkImage
}

fun checkImagesSize(image: BufferedImage, watermarkImage: BufferedImage) {
    if (image.width < watermarkImage.width || image.height < watermarkImage.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(-1)
    }
}

fun transpPercentInput() : Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    var transpPercent: Int = 0
    try{
        transpPercent = readln().toInt()
    }
    catch (ex: Exception){
        println("The transparency percentage isn't an integer number.")
        exitProcess(-1)
    }
    if (transpPercent < 0 || transpPercent > 100) {
        println("The transparency percentage is out of range.")
        exitProcess(-1)
    }
    return transpPercent
}

fun alphaSettings(image: BufferedImage) : Pair<Boolean, Color?> {
    var useAlphaChannel = false
    var color: Color? = null
    if (image.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        return Pair(readln().lowercase() == "yes", color)
    }
    else {
        println("Do you want to set a transparency color?")

        if (readln().lowercase() == "yes") {
            var colors = intArrayOf()
            useAlphaChannel = true
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val input = readln().split(" ").map{ it.toIntOrNull() }
            if (input.size == 3) {
                for (color in input){
                    if (color == null || color < 0 || color > 255){
                        println("The transparency color input is invalid.")
                        exitProcess(-1)
                    }
                    else{
                        colors += color
                    }
                }
                color = Color(colors[0], colors[1], colors[2])
            }
            else{
                println("The transparency color input is invalid.")
                exitProcess(-1)
            }
        }
        return Pair(useAlphaChannel, color)
    }
}

fun getPositionMethod(image: BufferedImage, watermarkImage: BufferedImage): Point {
    println("Choose the position method (single, grid):")
    var point = Point(-1, -1)
    when(readln()) {
        "single"-> {
            val diffX: Int = image.width - watermarkImage.width
            val diffY: Int = image.height - watermarkImage.height
            println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")
            val input = readln().split(" ").map{
                val value = it.toIntOrNull()
                if (value == null ){
                    println("The position input is invalid.")
                    exitProcess(-1)
                }
                value
            }
            if (input.size != 2){
                println("The position input is invalid.")
                exitProcess(-1)
            }
            if (input[0] > diffX || input[1] > diffY || input[0] < 0 || input[1] < 0) {
                println("The position input is out of range.")
                exitProcess(-1)
            }
            point = Point(input[0], input[1])
        }
        "grid"-> {}
        else -> {
            println("The position method input is invalid.")
            exitProcess(-1)
        }
    }
    return point
}

