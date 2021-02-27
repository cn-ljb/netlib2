package net.ljb.kt.interceptor

import android.util.Log
import net.ljb.kt.client.HttpClient
import okhttp3.Interceptor
import okhttp3.Response
import java.lang.Exception

/**
 * Author:Ljb
 * Time:2018/8/9
 * There is a lot of misery in life
 **/
class AddGlobalParamInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val paramMap = HttpClient.getHttpConfig()?.getCommParam()
            val headerMap = HttpClient.getHttpConfig()?.getCommHeader()
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
            Log.e("net", "add params error", e)
            throw e
        }
    }

}