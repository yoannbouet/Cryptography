package cryptography

import java.awt.Color
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.experimental.xor

enum class Messages(val str: String) {
    SizeError("The input image is not large enough to hold this message."),
    ReadError("Can't read input file!"),
    Menu("Task (hide, show, exit):"),
    WrongMenu("Wrong task: "),
    Exit("Bye!")
}

class Cryptography {
    private val messageEndBytes = mutableListOf<String>()

    init {
        while (true) {
            println(Messages.Menu.str)
            when (val action = readln()) {
                "hide" -> hide()
                "show" -> show()
                "exit" -> {
                    println(Messages.Exit.str)
                    break
                }
                else -> println(Messages.WrongMenu.str + action)
            }
        }
    }

    private fun show() {
        val (inputPrompt, inputFile) = Pair("Input image file:".also { println(it) }, File(readln()))
        val (passwordPrompt, password) = Pair("Password:".also { println(it) }, readln())
        val passwordBytes = mutableListOf<String>()
        password.forEach { passwordBytes.add(it.code.toByte().toString(2).padStart(8, '0')) }
        if (!isStrFileReadable(inputFile)) { println(Messages.ReadError.str); return }

        val image = ImageIO.read(inputFile)
        val messageBits = mutableListOf<Char>()
        val messageEndStr = messageEndBytes.joinToString("")
        val messageEndStrCmpr = StringBuilder()
        outer@ for (i in 0 until image.height) {
            inner@ for (j in 0 until image.width) {
                if (messageEndStr == messageEndStrCmpr.toString()) break@outer
                val color = Color(image.getRGB(j, i))
                val blueColorLastBit = if (color.blue % 2 == 0) '0' else '1'
                if (messageEndStrCmpr.length == 24) messageEndStrCmpr.delete(0,1)
                messageEndStrCmpr.append(blueColorLastBit)
                messageBits.add(blueColorLastBit)
            }
        }

        val messageBytes = messageBits.joinToString("").chunked(8)
        while (passwordBytes.size < messageBytes.size) for (i in 0..messageBytes.lastIndex) {
            if (passwordBytes.size < messageBytes.size) passwordBytes.add(passwordBytes[i])
        }

        // DECRYPTION
        val message = mutableListOf<Char>()
        for (i in 0..messageBytes.lastIndex) {
            message.add(Integer.parseInt((Integer.parseInt(messageBytes[i], 2).toChar().code.toByte().
            xor(Integer.parseInt(passwordBytes[i], 2).toChar().code.toByte())).toString(2), 2).toChar())
        }

        println("Message: ${message.joinToString("").substring(0, message.lastIndex - 2)}")
    }

    private fun hide() {
        val (inputPrompt, inputFile) = Pair("Input image file:".also { println(it) }, File(readln()))
        val (outputPrompt, outputFile) = Pair("Output image file:".also { println(it) }, File(readln()))
        val (messagePrompt, message) = Pair("Message to hide:".also { println(it) }, readln())
        val (passwordPrompt, password) = Pair("Password: ".also { println(it) }, readln())
        if (!isStrFileReadable(inputFile)) { println(Messages.ReadError.str); return }

        val messageBytes = mutableListOf<String>()
        val passwordBytes = mutableListOf<String>()
        message.forEach { messageBytes.add(it.code.toString(2).padStart(8, '0')) }
        password.forEach { passwordBytes.add(it.code.toString(2).padStart(8, '0')) }
        while (passwordBytes.size < messageBytes.size) for (i in 0..messageBytes.lastIndex) {
            if (passwordBytes.size < messageBytes.size) passwordBytes.add(passwordBytes[i])
        }

        // ENCRYPTION
        for (i in 0..messageBytes.lastIndex) {
            val messageByte = Integer.parseInt(messageBytes[i], 2).toChar().code.toByte().
            xor(Integer.parseInt(passwordBytes[i], 2).toChar().code.toByte())
            messageBytes[i] = messageByte.toString(2).padStart(8, '0')
        }

        // END BYTES
        "0".toByte().toInt().toString(2).padStart(8, '0').let {
            messageBytes.add(it); messageBytes.add(it)
            messageEndBytes.add(it); messageEndBytes.add(it)
        }
        "3".toByte().toInt().toString(2).padStart(8, '0').let {
            messageBytes.add(it); messageEndBytes.add(it)
        }

        val messageBits = messageBytes.joinToString("").toMutableList()

        val image = ImageIO.read(inputFile)
        if (image.width * image.height < (message.length + 3) * 8) { println(Messages.SizeError.str); return }
        for (i in 0 until image.height) {
            for (j in 0 until image.width) {
                val color = Color(image.getRGB(j, i))
                val blueColor = if (messageBits.isEmpty()) color.blue else {
                    (color.blue.and(254).or(messageBits.first().digitToInt())).also {
                        messageBits.removeAt(0)
                    }
                }
                image.setRGB(j, i, Color(color.red, color.green, blueColor).rgb)
            }
        }
        ImageIO.write(image, "png", outputFile)

        println("Message saved in ${outputFile.name} image.")
    }

    private fun isStrFileReadable(file: File): Boolean {
        try {
            ImageIO.read(file)
        } catch (e: IOException){
            return false
        }
        return true
    }
}

fun main() {
    Cryptography()
}