package dst.lib.io

import play.api.Play

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

import org.apache.commons.io.IOUtils

import java.nio.file.{Path, Paths}
import java.nio.file.Files

object FtpClient {
  sealed trait FileType
  case object BinaryFile extends FileType
  case object AsciiFile extends FileType
}

class FtpClient(host: String, port: Int, user: String, password: String) {
  import FtpClient._

  def fetchFile(remoteFile: String, localDirectory: Path, fileType: FileType = AsciiFile)(implicit context: ExecutionContext): Future[Path] = {
    Files.createDirectories(localDirectory)

    val remoteFilePath = Paths.get(remoteFile)
    val remoteDirectory = remoteFilePath.getParent.toString

    for {
      ftpClient         <- connect()(context)
      optionsSet        <- future {
        setFileType(ftpClient, fileType)
        ftpClient.enterLocalPassiveMode()
        ftpClient.setAutodetectUTF8(true)
        ftpClient.changeWorkingDirectory(remoteDirectory)
      }
      localFile         <- downloadFile(remoteFilePath.getFileName.toString, localDirectory)(ftpClient, context)
      loggedOut         <- future{ ftpClient.logout }
    } yield localFile
  }


  def fetchFiles(remoteDirectory: String, localDirectory: Path, fileType: FileType = AsciiFile, filter: (String) => Boolean = _ => true)(implicit context: ExecutionContext): Future[List[Path]] = {
    Files.createDirectories(localDirectory)

    for {
      ftpClient         <- connect()(context)
      remoteFiles       <- listFiles(remoteDirectory, filter)(ftpClient, context)
      optionsSet        <- future {
        setFileType(ftpClient, fileType)
        ftpClient.enterLocalPassiveMode()
        ftpClient.setAutodetectUTF8(true)
        ftpClient.changeWorkingDirectory(remoteDirectory)
      }
      localFiles        <- serialiseFutures(remoteFiles) { remoteFile => downloadFile(remoteFile, localDirectory)(ftpClient, context) }
      loggedOut         <- future{ ftpClient.logout }
    } yield localFiles
  }

  private def connect()(implicit context: ExecutionContext): Future[FTPClient] = future {
    val ftpClient = new FTPClient()
    ftpClient.connect(host, port)

    val replyCode = ftpClient.getReplyCode
    if(!FTPReply.isPositiveCompletion(replyCode)) {
      throw new Exception(s"Failed to connect to ${host}:${port}, status code: ${replyCode}")
    }

    val loggedIn = ftpClient.login(user, password)
    if (!loggedIn) {
      throw new Exception(s"Failed to login to ${host}:${port}")
    }

    ftpClient
  }

  private def listFiles(remoteDirectory: String, filter: (String) => Boolean)(implicit ftpClient: FTPClient, context: ExecutionContext): Future[List[String]] = future {
    ftpClient.listNames(remoteDirectory).filter(filter).toList
  }

  private def setFileType(ftpClient: FTPClient, fileType: FileType)(implicit context: ExecutionContext): Boolean = {
    fileType match {
      case BinaryFile => ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
      case _ => ftpClient.setFileType(FTP.ASCII_FILE_TYPE)
    }
  }

  private def downloadFile(remoteFile: String, localDirectory: Path)(implicit ftpClient: FTPClient, context: ExecutionContext): Future[Path] = future {
    val remoteName = Paths.get(remoteFile).getFileName.toString
    val outputPath = localDirectory.resolve(remoteName).normalize
    val outputStream = Files.newOutputStream(outputPath);

    try {
      val inputStream = ftpClient.retrieveFileStream(remoteName);
      IOUtils.copy(inputStream, outputStream);
      outputStream.flush();
      inputStream.close()
      if (!ftpClient.completePendingCommand()) {
        throw new Exception(s"File transfer failed for $remoteFile")
      }
    } finally {
      outputStream.close()
    }

    outputPath
  }

  private def serialiseFutures[A, B](l: Iterable[A])(fn: A ⇒ Future[B])(implicit ec: ExecutionContext): Future[List[B]] = {
    l.foldLeft(Future(List.empty[B])) {
      (previousFuture, next) =>
      for {
        previousResults <- previousFuture
        next <- fn(next)
      } yield previousResults :+ next
    }
  }
}
