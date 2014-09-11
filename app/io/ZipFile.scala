package dst.lib.io

import scala.concurrent._

import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.{Path, Paths}

import java.util.zip.ZipInputStream
import java.util.zip.GZIPInputStream

import org.apache.commons.io.IOUtils
import org.apache.commons.io.FilenameUtils

object ZipFile {
  def unzip(zipFilePath: Path, unzipDirectory: Path)(implicit context: ExecutionContext): Future[List[Path]] = future {
    Files.createDirectories(unzipDirectory)

    val zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath.toString))
    try {
      val extractedFiles = Stream.continually(zipInputStream.getNextEntry)
        .takeWhile(_ != null)
        .map { zipEntry =>
          val outputPath = unzipDirectory.resolve(zipEntry.getName)
          val outputStream = Files.newOutputStream(outputPath);

          try {
            IOUtils.copy(zipInputStream, outputStream)
            outputStream.flush();
          } finally {
            outputStream.close()
          }

          outputPath
        }
        .toList

      extractedFiles
    }
    finally {
      zipInputStream.close()
    }
  }

  def ungzipFile(gzipFilePath: Path, unzipDirectory: Path)(implicit context: ExecutionContext): Future[Path] = future {
    val zipInputStream = new GZIPInputStream(new FileInputStream(gzipFilePath.toString))

    Files.createDirectories(unzipDirectory)

    val outputPath = unzipDirectory.resolve(FilenameUtils.removeExtension(gzipFilePath.getFileName.toString))
    val outputStream = Files.newOutputStream(outputPath);

    try {
      IOUtils.copy(zipInputStream, outputStream)
      outputStream.flush();
    } finally {
      outputStream.close()
    }

    outputPath
  }
}
