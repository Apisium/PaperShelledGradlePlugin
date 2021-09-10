@file:Suppress("UnstableApiUsage")

package cn.apisium.papershelled.gradle

import java.net.URL
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.DateUtils
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.file.RegularFile
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.net.URI

val ProjectLayout.cache: Path
    get() = projectDirectory.file(".gradle/caches/papershelled").asFile.toPath()
fun ProjectLayout.getCache(file: String): RegularFile = projectDirectory.file(".gradle/caches/papershelled/$file")
fun ProjectLayout.getCaches(file: String) = projectDirectory.files(".gradle/caches/papershelled/$file")

abstract class DownloadService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val httpClient: CloseableHttpClient = HttpClientBuilder.create().let { builder ->
        builder.setRetryHandler { _, count, _ -> count < 3 }
        builder.useSystemProperties()
        builder.build()
    }

    @ExperimentalPathApi
    fun download(source: URL, target: Path) {
        target.parent.createDirectories()

        val etagDir = target.resolveSibling("etags")
        etagDir.createDirectories()

        val etagFile = etagDir.resolve(target.name + ".etag")
        val etag = if (etagFile.exists()) etagFile.readText() else null

        val host = HttpHost(source.host, source.port, source.protocol)
        val time = if (target.exists()) target.getLastModifiedTime().toInstant() else Instant.EPOCH

        val httpGet = HttpGet(source.file)
        // high timeout, reduce chances of weird things going wrong
        val timeouts = TimeUnit.MINUTES.toMillis(5).toInt()

        httpGet.config = RequestConfig.custom()
            .setConnectTimeout(timeouts)
            .setConnectionRequestTimeout(timeouts)
            .setSocketTimeout(timeouts)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build()

        if (target.exists()) {
            if (time != Instant.EPOCH) {
                val value = DateTimeFormatter.RFC_1123_DATE_TIME.format(time.atZone(ZoneOffset.UTC))
                httpGet.setHeader("If-Modified-Since", value)
            }
            if (etag != null) {
                httpGet.setHeader("If-None-Match", etag)
            }
        }

        httpClient.execute(host, httpGet).use { response ->
            val code = response.statusLine.statusCode
            if (code !in 200..299 && code != HttpStatus.SC_NOT_MODIFIED) {
                val reason = response.statusLine.reasonPhrase
                throw RuntimeException("Download failed, HTTP code: $code; URL: $source; Reason: $reason")
            }

            val lastModified = handleResponse(response, target)
            saveEtag(response, lastModified, target, etagFile)
        }
    }

    @ExperimentalPathApi
    private fun handleResponse(response: CloseableHttpResponse, target: Path): Instant {
        val lastModified = with(response.getLastHeader("Last-Modified")) {
            if (this == null) {
                return@with Instant.EPOCH
            }
            if (value.isNullOrBlank()) {
                return@with Instant.EPOCH
            }
            return@with DateUtils.parseDate(value).toInstant() ?: Instant.EPOCH
        }
        if (response.statusLine.statusCode == HttpStatus.SC_NOT_MODIFIED) {
            return lastModified
        }

        val entity = response.entity ?: return lastModified
        target.outputStream().use { output ->
            entity.content.use { input ->
                input.copyTo(output)
            }
        }

        return lastModified
    }

    @ExperimentalPathApi
    private fun saveEtag(response: CloseableHttpResponse, lastModified: Instant, target: Path, etagFile: Path) {
        if (lastModified != Instant.EPOCH) {
            target.setLastModifiedTime(FileTime.from(lastModified))
        }

        val header = response.getFirstHeader("ETag") ?: return
        val etag = header.value

        etagFile.writeText(etag)
    }

    override fun close() {
        httpClient.close()
    }
}

interface DownloadParams : WorkParameters {
    val source: Property<String>
    val target: RegularFileProperty
    val downloader: Property<DownloadService>
}

abstract class DownloadWorker : WorkAction<DownloadParams> {
    @ExperimentalPathApi
    override fun execute() {
        parameters.downloader.get().download(URI.create(parameters.source.get()).toURL(),
            parameters.target.get().asFile.toPath())
    }
}
