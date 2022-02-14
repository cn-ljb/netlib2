package net.ljb.kt.interceptor

import net.ljb.kt.utils.StringUtils
import okhttp3.*
import okhttp3.internal.http.promisesBody
import okhttp3.internal.platform.Platform
import okio.Buffer
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import net.ljb.kt.utils.JsonParser


open class NetworkLoggingInterceptor @JvmOverloads constructor(private val logger: Logger = Logger.DEFAULT) :
    Interceptor {
    enum class Level {
        /**
         * No logs.
         */
        NONE,

        /**
         * Logs request and response lines.
         *
         *
         * Example:
         * <pre>`--> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
        `</pre> *
         */
        BASIC,

        /**
         * Logs request and response lines and their respective headers.
         *
         *
         * Example:
         * <pre>`--> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
        `</pre> *
         */
        HEADERS,

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         *
         * Example:
         * <pre>`--> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
        `</pre> *
         */
        BODY
    }

    interface Logger {
        fun log(message: String?)

        companion object {
            /**
             * A [Logger] defaults output appropriate for the current platform.
             */
            val DEFAULT: Logger = object : Logger {

                override fun log(message: String?) {
                    message?.run {
                        Platform.get().log(this, Platform.INFO, null)
                    }
                }
            }
        }
    }

    @Volatile

    var level = Level.NONE
        private set

    /**
     * Change the level at which this interceptor logs.
     */
    fun setLevel(level: Level?): NetworkLoggingInterceptor {
        if (level == null) throw NullPointerException("level == null. Use Level.NONE instead.")
        this.level = level
        return this
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = level
        val request: Request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }
        val logBody = level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS
        val requestBody = request.body
        val hasRequestBody = requestBody != null
        val connection: Connection? = chain.connection()
        var requestStartMessage: String = ("--> Request "
                + request.method
                + ' ' + request.url
                + if (connection != null) " " + connection.protocol() else "")
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody!!.contentLength() + "-byte body)"
        }
        logger.log(requestStartMessage)
        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody!!.contentType() != null) {
                    logger.log("Content-Type: " + requestBody.contentType())
                }
                if (requestBody.contentLength() != -1L) {
                    logger.log("Content-Length: " + requestBody.contentLength())
                }
            }
            // 打印 headers
            val headers = request.headers
            val sb = StringBuilder()
            headers.forEach {
                sb.append(it.first).append(":").append(it.second).append(",")
            }
            if (sb.isNotEmpty()) {
                sb.delete(sb.length - 1, sb.length)
            }
            logger.log("| RequestHeaders: {" + StringUtils.urlDecode(sb.toString()) + "}")

            // 打印Params
            val method = request.method
            if ("POST".equals(method, ignoreCase = true)) {
                // 打印所有post参数
                if (requestBody is FormBody) {
                    val sb = StringBuilder()
                    for (i in 0 until requestBody.size) {
                        sb.append(requestBody.encodedName(i)).append(":")
                            .append(requestBody.encodedValue(i))
                            .append(",")
                    }
                    if (sb.isNotEmpty()) {
                        sb.delete(sb.length - 1, sb.length)
                    }
                    logger.log("| RequestParams: {" + StringUtils.urlDecode(sb.toString()) + "}")
                } else {
                    //logger.log("| RequestParams: {" + StringUtils.urlDecode(requestBody.toString()) + "}")
                    logger.log("| RequestParams: ${JsonParser.toJson(requestBody!!)}")
                }
            } else if ("GET".equals(method, ignoreCase = true)) {
                // 打印所有get参数
                val sb = StringBuilder()
                val paramKeys = request.url.queryParameterNames
                for (key: String? in paramKeys) {
                    val value = request.url.queryParameter(key!!)
                    sb.append(key).append(":").append(value).append(",")
                }
                if (sb.isNotEmpty()) {
                    sb.delete(sb.length - 1, sb.length)
                }
                logger.log("| RequestParams: {" + StringUtils.urlDecode(sb.toString()) + "}")
            }
            if (!logBody || !hasRequestBody) {
                logger.log("--> END " + request.method)
            } else if (bodyEncoded(request.headers)) {
                logger.log("--> END " + request.method + " (encoded body omitted)")
            } else {
                val buffer = Buffer()
                requestBody!!.writeTo(buffer)
//                var charset: Charset? = UTF8
//                val contentType = requestBody.contentType()
//                if (contentType != null) {
//                    charset = contentType.charset(UTF8)
//                }
                logger.log("")
                if (isPlaintext(buffer)) {
//                    logger.log(buffer.readString(charset));
                    logger.log(
                        "--> END " + request.method
                                + " (" + requestBody.contentLength() + "-byte body)"
                    )
                } else {
                    logger.log(
                        ("--> END " + request.method + " (binary "
                                + requestBody.contentLength() + "-byte body omitted)")
                    )
                }
            }
        }
        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.log("<-- HTTP FAILED: $e")
            throw e
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        val responseBody = response.body
        val contentLength = responseBody!!.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        logger.log(
            ("<-- "
                    + response.code
                    + (if (response.message.isEmpty()) "" else ' '.toString() + response.message)
                    + ' ' + response.request.url
                    + " (" + tookMs + "ms" + (if (!logHeaders) ", $bodySize body" else "") + ')')
        )
        if (logHeaders) {
            val headers = response.headers
            val sb = StringBuilder()
            for (i in 0 until headers.size) {
                val name = headers.name(0)
                sb.append(name).append(":").append(headers.get(name)).append(",")
            }
            if (sb.isNotEmpty()) {
                sb.delete(sb.length - 1, sb.length)
            }
            logger.log("| ResponseHeaders: {" + StringUtils.urlDecode(sb.toString()) + "}")

            if (!logBody || !response.promisesBody()) {
                logger.log("<-- END HTTP")
            } else if (bodyEncoded(response.headers)) {
                logger.log("<-- END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                val buffer = source.buffer()
                var charset: Charset? = UTF8
                val contentType = responseBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(UTF8)
                }
                if (!isPlaintext(buffer)) {
                    logger.log("")
                    logger.log("<-- END HTTP (binary " + buffer.size + "-byte body omitted)")
                    return response
                }
                if (contentLength != 0L) {
                    logger.log("")
                    var info = StringUtils.urlDecode(
                        buffer.clone().readString((charset!!))
                    )
                    info = StringUtils.unicodeToString(info)
                    if (info.length > 4000) {
                        var i = 0
                        while (i < info.length) {
                            val end = if (i + 4000 > info.length) info.length else i + 4000
                            val subStr = info.substring(i, end)
                            if (i == 0) {
                                logger.log("| ResponseResult: $subStr")
                            } else {
                                logger.log(subStr)
                            }
                            i = end
                        }
                    } else {
                        logger.log("| ResponseResult: $info")
                    }
                }
                logger.log("<-- END HTTP (" + buffer.size + "-byte body)")
            }
        }
        return response
    }

    private fun bodyEncoded(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"]
        return contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)
    }

    companion object {
        private val UTF8: Charset = Charset.forName("UTF-8")!!

        /**
         * Returns true if the body in question probably contains human readable text. Uses a small sample
         * of code points to detect unicode control characters commonly used in binary file signatures.
         */
        fun isPlaintext(buffer: Buffer): Boolean {
            try {
                val prefix = Buffer()
                val byteCount = if (buffer.size < 64) buffer.size else 64
                buffer.copyTo(prefix, 0, byteCount)
                for (i in 0..15) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                return true
            } catch (e: EOFException) {
                return false // Truncated UTF-8 sequence.
            }
        }
    }
}
