package net.ljb.kt.file.request

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.Source
import okio.source
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

class FileUploadProgressBody(
    private var file: File,
    private var mediaType: String,
    private var listener: ProgressListener
) : RequestBody() {

    companion object {
        const val SEGMENT_SIZE = 10 * 1024L // okio.Segment.SIZE
    }

    interface ProgressListener {

        /**
         * 上传进度
         * */
        fun uploadProgress(
            curSize: Long,
            totalSize: Long,
            progress: Int
        )
    }


    override fun contentLength(): Long {
        return file.length()
    }

    override fun contentType(): okhttp3.MediaType? {
        return mediaType.toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = file.source()
            var curSize: Long = 0
            var read: Long
            while (source.read(
                    sink.buffer(),
                    SEGMENT_SIZE
                )
                    .also { read = it } != -1L
            ) {
                curSize += read
                sink.flush()
                listener.uploadProgress(
                    curSize,
                    contentLength(),
                    (curSize / contentLength() * 100).toInt()
                )
            }
        } finally {
            source?.closeQuietly()
        }
    }


}