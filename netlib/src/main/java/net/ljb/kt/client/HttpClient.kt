package net.ljb.kt.client

import net.ljb.kt.HttpConfig
import net.ljb.kt.interceptor.AddGlobalParamInterceptor
import net.ljb.kt.interceptor.HttpLoggingInterceptor
import net.ljb.kt.utils.JsonParser
import net.ljb.kt.utils.NetLog
import okhttp3.Response
import okhttp3.Request
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.Callback
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager


/**
 * HttpClient
 * 1、可使用 OkHttp
 * 2、也可使用 Retrofit
 * Created by L on 2017/6/8.
 */
object HttpClient {

    private const val DEFAULT_TIME_OUT = 60000L
    private const val DEFAULT_DOWN_TIME_OUT = 60000L * 5

    private var mHttpConfig: HttpConfig? = null

    fun init(httpConfig: HttpConfig) {
        mHttpConfig = httpConfig
    }

    fun getHttpConfig() = mHttpConfig

    private val mRetrofit by lazy {
        Retrofit.Builder()
            .client(mHttpClient)
            .baseUrl(mHttpConfig!!.baseUrl)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val mStrRetrofit by lazy {
        Retrofit.Builder()
            .client(mHttpClient)
            .baseUrl(mHttpConfig!!.baseUrl)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(StringConverterFactory())
            .build()
    }

    private val mHttpClient by lazy {

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(HttpLoggingInterceptor {
                NetLog.i(it)
            }.setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(AddGlobalParamInterceptor())
            .connectTimeout(DEFAULT_TIME_OUT, TimeUnit.MILLISECONDS)

        //是否持久化cookie
        mHttpConfig!!.commCookie?.run {
            builder.cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (cookies.isNullOrEmpty()) {
                        saveCookie(url.host, "")
                        return
                    }
                    saveCookie(url.host, JsonParser.toJson(cookies))
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val loadCookie = loadCookie(url.host)
                    if (loadCookie.isNullOrEmpty()) {
                        return listOf()
                    }
                    return JsonParser.fromJsonArr(loadCookie, Cookie::class.java)
                }
            })
        }
        builder.build()
    }

    private val mLongTimeHttpClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(AddGlobalParamInterceptor())
            .connectTimeout(DEFAULT_DOWN_TIME_OUT, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_DOWN_TIME_OUT, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun createSSLSocketFactory(): SSLSocketFactory {
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, arrayOf(TrustAllCerts()), java.security.SecureRandom())
        return sc.socketFactory
    }

    private class TrustAllCerts : X509TrustManager {

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate?> {
            return arrayOfNulls(0)
        }
    }

    fun getHttpClient(): OkHttpClient = mHttpClient

    fun getLongHttpClient() = mLongTimeHttpClient

    /**
     * Retrofit
     * */
    fun getRetrofit(): Retrofit = mRetrofit

    /**
     * StrRetrofit
     * */
    fun getStrRetrofit(): Retrofit = mStrRetrofit

    /**
     * 同步
     * */
    fun execute(request: Request): Response = mHttpClient.newCall(request).execute()

    /**
     * 异步
     * */
    fun enqueue(request: Request, responseCallback: Callback) =
        mHttpClient.newCall(request).enqueue(responseCallback)

    /**
     * 创建Request
     * */
    fun getRequest(url: String, method: HttpMethod, params: Map<String, String>?): Request {
        val builder = Request.Builder()
        if (HttpMethod.GET == method) {
            builder.url(initGetRequest(url, params ?: HashMap())).get()
        } else if (HttpMethod.POST == method) {
            builder.url(url).post(initRequestBody(params ?: HashMap()))
        } else if (HttpMethod.PUT == method) {
            builder.url(url).put(initRequestBody(params ?: HashMap()))
        } else if (HttpMethod.DELETE == method) {
            if (params == null || params.isEmpty()) {
                builder.url(url).delete()
            } else {
                builder.url(url).delete(initRequestBody(params))
            }
        }
        return builder.build()
    }


    /**
     * 创建Body请求体
     * */
    private fun initRequestBody(params: Map<String, String>): RequestBody {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        params.map { bodyBuilder.addFormDataPart(it.key, it.value) }
        return bodyBuilder.build()
    }


    /**
     * 创建Get链接
     * */
    private fun initGetRequest(url: String, params: Map<String, String>): String {
        return StringBuilder(url).apply {
            if (params.isNotEmpty()) {
                append("?")
                params.map {
                    append(it.key).append("=").append(it.value).append("&")
                }
                delete(length - "&".length, length)
            }
        }.toString()
    }


}


