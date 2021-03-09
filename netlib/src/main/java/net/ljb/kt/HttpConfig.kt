package net.ljb.kt

import net.ljb.kt.client.HttpClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.*
import kotlin.collections.ArrayList

class HttpConfig private constructor(
    val baseUrl: String,
    private val commHeader: ((headers: Map<String, String>) -> Unit)?,
    private val commParam: ((params: Map<String, String>) -> Unit)?,
    val commCookie: ICommCookie?,
    val isOpenLog: Boolean,
    var tag: String,
    var interceptors: MutableList<Interceptor>?
) {

    var client: OkHttpClient? = null

    interface ICommCookie {
        fun saveCookie(host: String, cookie: String)
        fun loadCookie(host: String): String?
    }

    fun getCommParam(): Map<String, String>? {
        if (commParam == null) return null
        val map = HashMap<String, String>()
        commParam.invoke(map)
        return map
    }

    fun getCommHeader(): Map<String, String>? {
        if (commHeader == null) return null
        val map = HashMap<String, String>()
        commHeader.invoke(map)
        return map
    }

    class Builder(private val baseUrl: String) {

        private var openLog = false
        private var commHeader: ((headers: Map<String, String>) -> Unit)? = null
        private var commParam: ((params: Map<String, String>) -> Unit)? = null
        private var commCookie: ICommCookie? = null
        private var tag: String = HttpClient.TAG_DEFAULT_CLIENT
        private var interceptors: MutableList<Interceptor>? = null

        fun addCommCookie(cookie: ICommCookie?): Builder {
            commCookie = cookie
            return this
        }

        fun addCommHeader(header: ((params: Map<String, String>) -> Unit)): Builder {
            commHeader = header
            return this
        }

        fun addCommParam(param: ((params: Map<String, String>) -> Unit)): Builder {
            commParam = param
            return this
        }

        fun openLog(open: Boolean): Builder {
            openLog = open
            return this
        }

        fun newClientByTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun addInterceptor(interceptor: Interceptor): Builder {
            if (interceptors == null) {
                interceptors = ArrayList()
            }
            interceptors!!.add(interceptor)
            return this
        }

        fun build() {
            HttpClient.init(
                HttpConfig(
                    baseUrl,
                    commHeader,
                    commParam,
                    commCookie,
                    openLog,
                    tag,
                    interceptors
                )
            )
        }
    }
}