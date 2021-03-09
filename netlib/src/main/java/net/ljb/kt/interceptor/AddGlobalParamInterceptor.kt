package net.ljb.kt.interceptor

import net.ljb.kt.client.HttpClient
import net.ljb.kt.utils.NetLog
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Author:Ljb
 * Time:2018/8/9
 * There is a lot of misery in life
 **/
class AddGlobalParamInterceptor(private val tag: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val paramMap = HttpClient.getHttpConfig()[tag]?.getCommParam()
            val headerMap = HttpClient.getHttpConfig()[tag]?.getCommHeader()
            val oldRequest = chain.request()
            val newRequestBuilder = oldRequest.newBuilder()

            // 添加公共Header
            if (headerMap != null && headerMap.isNotEmpty()) {
                val newHeaderBuilder = oldRequest.headers.newBuilder()
                headerMap.map { newHeaderBuilder.add(it.key, it.value) }
                newRequestBuilder.headers(newHeaderBuilder.build())
            }

            // 添加公共的Param
            if (paramMap != null && paramMap.isNotEmpty()) {
                val newUrlBuilder = oldRequest.url.newBuilder()
                newUrlBuilder.scheme(oldRequest.url.scheme)
                newUrlBuilder.host(oldRequest.url.host)
                paramMap.map { newUrlBuilder.addQueryParameter(it.key, it.value) }
                newRequestBuilder.url(newUrlBuilder.build())
            }

            // 新的请求
            val newRequest = newRequestBuilder
                .method(oldRequest.method, oldRequest.body)
                .build()
            return chain.proceed(newRequest)
        } catch (e: Exception) {
            NetLog.e(e)
            throw e
        }
    }

}