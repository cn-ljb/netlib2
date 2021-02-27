package net.ljb.kt.file

import android.content.Context
import android.os.Handler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ljb.kt.client.HttpClient
import net.ljb.kt.utils.RxUtils
import okhttp3.Call
import okhttp3.Request
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException

/**
 * 文件下载
 * */
class FileDownloadTask private constructor() {

    interface FileDownloadListener {

        /**
         * 开始下载
         * */
        fun downloadStart()

        /**
         * 下载进度
         * */
        fun downloadProgress(
            curSize: Long,
            totalSize: Long,
            progress: Int
        )

        /**
         * 下载成功
         * */
        fun downloadSuccess(path: String)

        /**
         * 下载失败
         * */
        fun downloadError(it: Throwable)
    }

    private lateinit var downUrl: String
    private lateinit var savePath: String
    private var context: Context? = null
    private var listener: FileDownloadListener? = null

    private var downloadDisposable: Disposable? = null

    private val handler: Handler by lazy {
        Handler(context!!.mainLooper)
    }

    /**
     * 开始下载任务
     * */
    fun execute(): FileDownloadTask {
        listener?.downloadStart()
        val request = Request.Builder().url(downUrl).get().build()
        val call = HttpClient.getLongHttpClient().newCall(request)
        Observable.create<String> {
            try {
                download(call, savePath, listener)
                it.onNext(savePath)
                it.onComplete()
            } catch (e: java.lang.Exception) {
                it.onError(e)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {



                override fun onComplete() {
                    release()
                }


                override fun onSubscribe(d: Disposable) {
                    downloadDisposable = d
                }

                override fun onNext(t: String) {
                    listener?.downloadSuccess(t)

                }

                override fun onError(e: Throwable) {
                    listener?.downloadError(e)
                    release()
                }



            })

        return this
    }

    /**
     * 文件下载
     * */
    private fun download(
        call: Call, savePath: String,
        listener: FileDownloadListener?
    ) {
        val response = call.execute()
        if (response.isSuccessful) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val totalSize = response.body!!.contentLength()
                inputStream = response.body!!.byteStream()
                outputStream = FileOutputStream(savePath)
                val buff = ByteArray(1024 * 1024)
                var curSize: Long = 0
                var len = inputStream.read(buff)
                while (len != -1) {
                    outputStream.write(buff, 0, len)
                    curSize += len.toLong()
                    len = inputStream.read(buff)

                    //更新UI进度
                    listener?.run {
                        val curProgress = (curSize.toFloat() / totalSize.toFloat() * 100f).toInt()
                        handler.post {
                            this.downloadProgress(curSize, totalSize, curProgress)
                        }
                    }
                }
            } catch (e: Exception) {
                throw  e
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } else {
            throw IllegalStateException(response.message)
        }
    }


    /**
     * 释放资源
     * */
    fun release() {
        RxUtils.dispose(downloadDisposable)
        listener = null
        context = null
    }

    /**
     * 构造者
     * */
    class Builder(var downUrl: String, var savePath: String) {

        private var listener: FileDownloadListener? = null

        fun listener(listener: FileDownloadListener): Builder {
            this.listener = listener
            return this
        }

        fun build(context: Context): FileDownloadTask {
            val task = FileDownloadTask()
            task.context = context
            task.downUrl = downUrl
            task.savePath = savePath
            task.listener = listener
            return task
        }

    }

}