package dst.lib.io

import play.api.Play.current
import play.Logger


import play.api.libs.ws._

import scala.concurrent._
import scala.concurrent.duration._

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream

import org.apache.commons.io.IOUtils

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext


object HttpDownloader {
  def download(url: String, filePath: Path, headers: Option[Map[String, String]] = None)(implicit timeout: Duration, context: ExecutionContext): Future[Path] = {
    Logger.info(s"Downloading $url to $filePath")
    var request = headers.foldLeft(WS.url(url)) { (request, headers) => request.withHeaders((headers + ("Accept-Encoding" -> "gzip")).toSeq:_*)}

    val promise = request.withRequestTimeout(timeout.toMillis.toInt).get() map { response =>
      val outputStream = Files.newOutputStream(filePath);
      try {
        val inputStream = response .header("Content-Encoding") match {
          case Some("gzip") => new GZIPInputStream(response.getAHCResponse.getResponseBodyAsStream)
          case _ => response.getAHCResponse.getResponseBodyAsStream
        }

        IOUtils.copy(inputStream, outputStream);
        outputStream.flush();
        inputStream.close()

        filePath
      } finally {
        outputStream.close()
      }
    }

    promise.onFailure { case t: Throwable => Logger.info(t.toString) }
    promise
  }
}
