package net.ljb.kt

import net.ljb.kt.client.HttpClient.init
import java.util.*

class HttpConfig private constructor(
    val baseUrl: String,
    private val commHeader: ((headers: Map<String, String>) -> Unit)?,
    private val commParam: ((params: Map<String, String>) -> Unit)?,
    val commCookie: ICommCookie?,
    val isOpenLog: Boolean
) {

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

        fun build() {
            val httpConfig = HttpConfig(baseUrl, commHeader, commParam, commCookie, openLog)
            init(httpConfig)
        }
    }
}