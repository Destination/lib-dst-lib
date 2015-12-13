package dst.lib.io

import play.api.Play.current
import play.Logger

import play.api.libs.ws._
import com.ning.http.client.Realm.AuthScheme

import scala.concurrent._
import scala.concurrent.duration._

import java.nio.file.Files;
import java.nio.file.{Path, Paths};
import java.util.zip.GZIPInputStream

import org.apache.commons.io.IOUtils

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext


object HttpDownloader {
  def download(url: String, localDirectory: Path, headers: Option[Map[String, String]] = None, credentials: Option[(String, String)] = None)(implicit timeout: Duration, context: ExecutionContext): Future[Path] = {
    val remoteName = Paths.get(url).getFileName.toString
    val outputPath = localDirectory.resolve(remoteName).normalize

    Files.createDirectories(localDirectory)

    Logger.info(s"Downloading $url to $outputPath")

    def withHeaders(request: WS.WSRequestHolder): WS.WSRequestHolder = headers.foldLeft(request) { (request, headers) => request.withHeaders((headers + ("Accept-Encoding" -> "gzip")).toSeq:_*) }
    def withCredentials(request: WS.WSRequestHolder): WS.WSRequestHolder = credentials.foldLeft(request) { (request, credentials) => request.withAuth(credentials._1, credentials._2, AuthScheme.BASIC) }

    val request = withCredentials(withHeaders(WS.url(url)))

    val promise = request.withRequestTimeout(timeout.toMillis.toInt).get() map { response =>
      val outputStream = Files.newOutputStream(outputPath);
      try {
        val inputStream = response .header("Content-Encoding") match {
          case Some("gzip") => new GZIPInputStream(response.getAHCResponse.getResponseBodyAsStream)
          case _ => response.getAHCResponse.getResponseBodyAsStream
        }

        IOUtils.copy(inputStream, outputStream);
        outputStream.flush();
        inputStream.close()

        outputPath
      } finally {
        outputStream.close()
      }
    }

    promise.onFailure { case t: Throwable => Logger.info(t.toString) }
    promise
  }
}
