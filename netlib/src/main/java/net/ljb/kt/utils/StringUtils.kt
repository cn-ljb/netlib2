package net.ljb.kt.utils

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 字符串转换，使用包装类
 * Created by zhangzhang on 16/1/21.
 */
object StringUtils {

    /**
     * Return the string of decode urlencoded string.
     *
     * @param input       The input.
     * @param charsetName The name of charset.
     * @return the string of decode urlencoded string
     */
    /**
     * Return the string of decode urlencoded string.
     *
     * @param input The input.
     * @return the string of decode urlencoded string
     */
    @JvmOverloads
    @JvmStatic
    fun urlDecode(input: String?, charsetName: String? = "UTF-8"): String {
        return if (input == null || input.isEmpty()) "" else try {
            val safeInput = input.replace("%(?![0-9a-fA-F]{2})".toRegex(), "%25")
                .replace("\\+".toRegex(), "%2B")
            URLDecoder.decode(safeInput, charsetName)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
    }
    /**
     * Return the urlencoded string.
     *
     * @param input       The input.
     * @param charsetName The name of charset.
     * @return the urlencoded string
     */
    /**
     * Return the urlencoded string.
     *
     * @param input The input.
     * @return the urlencoded string
     */
    @JvmStatic
    @JvmOverloads
    fun urlEncode(input: String?, charsetName: String? = "UTF-8"): String {
        return if (input == null || input.isEmpty()) "" else try {
            URLEncoder.encode(input, charsetName)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
    }

    @JvmStatic
    fun unicodeToString(s: String): String {
        var str = s
        val pattern: Pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))")
        val matcher: Matcher = pattern.matcher(str)
        var ch: Char
        while (matcher.find()) {
            //group 6728
            val group: String = matcher.group(2)
            //ch:'木' 26408
            ch = group.toInt(16).toChar()
            //group1 \u6728
            val group1: String = matcher.group(1)
            str = str.replace(group1, ch.toString() + "")
        }
        return str
    }

}