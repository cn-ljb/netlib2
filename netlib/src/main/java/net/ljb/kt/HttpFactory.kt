package net.ljb.kt

import net.ljb.kt.client.HttpClient
import java.util.*

/**
 * Created by L on 2017/6/8.
 */
object HttpFactory {

    private val mProtocolMap = WeakHashMap<Class<*>, Any>()

    fun <T> getProtocol(clazz: Class<T>): T {
        return getProtocol(clazz, HttpClient.TAG_DEFAULT_CLIENT)
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getProtocol(clazz: Class<T>, tag: String = HttpClient.TAG_DEFAULT_CLIENT): T {
        return if (mProtocolMap.contains(clazz)) {
            mProtocolMap[clazz] as T
        } else {
            val protocol = HttpClient.getRetrofitByTag(tag).create(clazz)
            mProtocolMap[clazz] = protocol
            protocol
        }
    }
}