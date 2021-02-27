package net.ljb.kt.file

import android.content.Context
import android.os.Handler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ljb.kt.client.HttpClient
import net.ljb.kt.file.request.FileUploadProgressBody
import net.ljb.kt.utils.RxUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


/**
 * 文件上传
 * */
class FileUploadTask private constructor() {


    interface FileUploadListener {

        /**
         * 开始上传
         * */
        fun uploadStart()

        /**
         * 上传进度
         * */
        fun uploadProgress(
            curSize: Long,
            totalSize: Long,
            progress: Int
        )

        /**
         * 上传成功
         * */
        fun uploadSuccess(result: String)

        /**
         * 上传失败
         * */
        fun uploadError(it: Throwable)
    }

    private var listener: FileUploadListener? = null
    private lateinit var url: String
    private lateinit var fileKey: String
    private lateinit var filePath: String
    private var params: Map<String, String>? = null
    private var mediaType: String = ""

    private var context: Context? = null

    private var upDisposable: Disposable? = null

    private val handler: Handler by lazy {
        Handler(context!!.mainLooper)
    }

    /**
     * 开始上传任务
     * */
    fun execute(): FileUploadTask {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        val file = File(filePath)

        //封装文件body
        val fileBody = getRequestBody(file)

        //文件参数
        builder.addFormDataPart(fileKey, file.name, fileBody)

        //其他参数
        params?.map {
            builder.addFormDataPart(it.key, it.value)
        }

        sendRequest(builder.build())
        return this
    }

    //上传文件
    private fun sendRequest(body: MultipartBody) {
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val newCall = HttpClient.getLongHttpClient().newCall(request)

        Observable.create<String> { it ->
            try {
                val response = newCall.execute()
                if (response.isSuccessful) {
                    val json = response.body!!.string()
                    it.onNext(json)
                } else {
                    it.onError(IllegalStateException(response.message))
                }
            } catch (e: Exception) {
                it.onError(e)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onComplete() {
                    release()
                }

                override fun onSubscribe(d: Disposable) {
                    upDisposable = d
                    listener?.uploadStart()
                }

                override fun onNext(t: String) {
                    listener?.uploadSuccess(t)
                }

                override fun onError(e: Throwable) {
                    listener?.uploadError(e)
                    release()
                }

            })
    }

    fun release() {
        RxUtils.dispose(upDisposable)
        listener = null
        context = null
    }


    /**
     * 封装File RequestBody
     * 依据当前是否有需要回调上传进度
     * */
    private fun getRequestBody(file: File): RequestBody {
        return if (listener != null) {
            FileUploadProgressBody(
                file,
                mediaType,
                object :
                    FileUploadProgressBody.ProgressListener {
                    override fun uploadProgress(curSize: Long, totalSize: Long, progress: Int) {
                        listener?.run {
                            handler.post {
                                this.uploadProgress(curSize, totalSize, progress)
                            }
                        }
                    }
                })
        } else {
            file.asRequestBody(mediaType.toMediaTypeOrNull())
        }
    }


    /**
     * 构造者
     * */
    class Builder(var url: String, var fileKey: String, var filePath: String) {

        private var listener: FileUploadListener? = null
        private var params: Map<String, String>? = null
        private var mediaType: String = ""

        fun listener(listener: FileUploadListener): Builder {
            this.listener = listener
            return this
        }

        fun params(params: Map<String, String>): Builder {
            this.params = params
            return this
        }

        fun mediaType(mediaType: String): Builder {
            this.mediaType = mediaType
            return this
        }

        fun build(context: Context): FileUploadTask {
            val task = FileUploadTask()
            task.context = context
            task.url = url
            task.fileKey = fileKey
            task.filePath = filePath
            task.listener = listener
            task.mediaType = mediaType
            task.params = params
            return task
        }

    }


}