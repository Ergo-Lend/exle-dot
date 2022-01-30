package Hermes

import org.ergoplatform.appkit.{InputBox, OutBox}

import java.io.{File, PrintWriter}

object Hermes {
  var textCount = 1
  def read(inputBoxes: List[InputBox], outputBoxes: List[OutBox]) = {
    var inputBox: InputBox = inputBoxes(0)
    var outputBox: OutBox = outputBoxes(0)

  }

  def write(fileContent: String): Unit = {
    writeToFile(s"Hermes ${textCount}.txt", fileContent)
    textCount += 1
  }

  def writeToFile(fileName: String, fileContent: String): Unit = {
    val printWriter = new PrintWriter(new File(fileName))
    printWriter.write(fileContent)
    printWriter.close()
  }
}
