package tpc.demo.operations

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import tpc.transactions.Operation

class AppendLogOperation(content: String) extends Operation {
  private val temporaryFile = File.createTempFile("tpc-log", ".tmp")
  private val temporaryFilePath = Paths.get(temporaryFile.getAbsolutePath)

  override def execute(): Unit = {
    val fileWriter = new BufferedWriter(new FileWriter(temporaryFile))
    val formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    fileWriter.write(s"$formattedDate\n")
    fileWriter.write(s"$content\n\n")
    fileWriter.close()
  }

  override def rollback(): Unit = cleanUp()

  override def commit(executorId: Int): Unit = {
    Files.write(Paths.get(s"${AppendLogOperation.logFilePath}$executorId"),
      Files.readAllBytes(temporaryFilePath), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    cleanUp()
  }

  private def cleanUp(): Unit = temporaryFile.delete()
}

object AppendLogOperation {
  private val logFilePath = ConfigFactory.load().getString("application.logFile")
}
