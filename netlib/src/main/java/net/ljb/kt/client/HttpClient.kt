package net.ljb.kt.client

import android.annotation.SuppressLint
import net.ljb.kt.HttpConfig
import net.ljb.kt.interceptor.AddGlobalParamInterceptor
import net.ljb.kt.interceptor.NetworkLoggingInterceptor
import net.ljb.kt.utils.JsonParser
import net.ljb.kt.utils.NetLog
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
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
 * Created by L on 2017/6/8.
 */
object HttpClient {

    const val TAG_DEFAULT_CLIENT = "default_client"

    private const val DEFAULT_TIME_OUT = 60000L

    private const val DEFAULT_DOWN_TIME_OUT = 60000L * 5

    private var mConfigMap = HashMap<String, HttpConfig>()
    private var mRetrofitMap = HashMap<String, Retrofit>()

    fun init(httpConfig: HttpConfig) {
        if (httpConfig.tag.isEmpty()) {
            httpConfig.tag = TAG_DEFAULT_CLIENT
        }

        mConfigMap[httpConfig.tag] = httpConfig

        if (httpConfig.client == null) {
            httpConfig.client = createOkHttpClient(httpConfig.tag)
            mRetrofitMap[httpConfig.tag] = createRetrofit(httpConfig)
        }
    }

    fun getHttpConfig() = mConfigMap


    private val mLongTimeHttpClient by lazy {
        createOkHttpLongClient(TAG_DEFAULT_CLIENT)
    }

    /**
     * 新建 OkHttpClient对象
     * */
    private fun createOkHttpClient(tag: String): OkHttpClient {

        val httpConfig = mConfigMap[tag]!!

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(AddGlobalParamInterceptor(httpConfig.tag))
            .connectTimeout(DEFAULT_TIME_OUT, TimeUnit.MILLISECONDS)

        //自定义拦截器
        httpConfig.interceptors?.map {
            builder.addInterceptor(it)
        }

        //最後添加Log拦截器
        builder.addInterceptor(NetworkLoggingInterceptor(object : NetworkLoggingInterceptor.Logger {
            override fun log(message: String?) {
                if (httpConfig.isOpenLog) {
                    NetLog.i(message)
                }
            }
        }).setLevel(NetworkLoggingInterceptor.Level.BODY))

        //是否持久化cookie
        httpConfig.commCookie?.run {
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

        return builder.build()
    }

    /**
     * 新建Retrofit对象
     * */
    private fun createRetrofit(httpConfig: HttpConfig): Retrofit {
        return Retrofit.Builder()
            .client(httpConfig.client!!)
            .baseUrl(httpConfig.baseUrl)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getHttpClient(tag: String = TAG_DEFAULT_CLIENT): OkHttpClient = mConfigMap[tag]!!.client!!

    fun getLongHttpClient() = mLongTimeHttpClient

    /**
     * 通过tag获取Retrofit对象
     * */
    fun getRetrofitByTag(tag: String): Retrofit {
        if (mConfigMap[tag] == null) {
            throw IllegalStateException("not found config by tag '$tag'")
        }

        if (mRetrofitMap[tag] == null) {
            throw IllegalStateException("not found retrofit by tag '$tag'")
        }

        return mRetrofitMap[tag]!!
    }

    private fun createOkHttpLongClient(tag: String): OkHttpClient {
        val httpConfig = mConfigMap[tag]!!
        return OkHttpClient.Builder()
            .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(AddGlobalParamInterceptor(httpConfig.tag))
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

        @SuppressLint("TrustAllX509TrustManager")
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate?> {
            return arrayOfNulls(0)
        }
    }

}


