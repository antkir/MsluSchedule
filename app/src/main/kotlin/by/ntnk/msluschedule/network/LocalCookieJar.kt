/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.utils.EMPTY_STRING
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.internal.Util
import timber.log.Timber
import java.net.HttpCookie
import java.util.*

/* Simplified/Kotlinified JavaNetCookieJar from OkHttp package with local threads support */
class LocalCookieJar : CookieJar {
    private val localThreadCookie = object : ThreadLocal<String>() {
        @Synchronized override fun initialValue(): String? = null
    }

    fun removeCookie() {
        Timber.d("Removed cookie from thread: %s", Thread.currentThread().name)
        localThreadCookie.remove()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (localThreadCookie.get() == null) {
            for (cookie in cookies) {
                if (cookie.name().equals("JSESSIONID", ignoreCase = true)) {
                    Timber.i("Saving cookie: %s - %s", cookie.name(), cookie.value())
                    localThreadCookie.set(cookie.toString())
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        Timber.d("Cookie thread: %s", Thread.currentThread().name)
        var cookies: MutableList<Cookie>? = null
        val cookieHandler = localThreadCookie.get()
        if (cookieHandler != null) {
            if (!cookieHandler.isEmpty()) {
                Timber.i("Getting cookie: %s", cookieHandler)
                cookies = ArrayList()
                cookies.addAll(decodeHeaderAsJavaNetCookies(url, cookieHandler))
            }
        }
        return if (cookies != null) Collections.unmodifiableList(cookies) else emptyList()
    }

    /**
     * Convert a request header to OkHttp's cookies via [HttpCookie]. That extra step handles
     * multiple cookies in a single request header, which [Cookie.parse] doesn't support.
     */
    private fun decodeHeaderAsJavaNetCookies(url: HttpUrl, header: String): List<Cookie> {
        val result = ArrayList<Cookie>()
        var pos = 0
        val limit = header.length
        var pairEnd: Int
        while (pos < limit) {
            pairEnd = Util.delimiterOffset(header, pos, limit, ";,")
            val equalsSign = Util.delimiterOffset(header, pos, pairEnd, '=')
            val name = Util.trimSubstring(header, pos, equalsSign)
            if (name.startsWith("$")) {
                pos = pairEnd + 1
                continue
            }

            // We have either name=value or just a name.
            var value = if (equalsSign < pairEnd) {
                Util.trimSubstring(header, equalsSign + 1, pairEnd)
            } else {
                EMPTY_STRING
            }

            // If the value is "quoted", drop the quotes.
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length - 1)
            }

            result.add(Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(url.host())
                    .build())
            pos = pairEnd + 1
        }
        return result
    }
}
