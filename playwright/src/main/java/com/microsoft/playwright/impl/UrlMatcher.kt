/*
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.playwright.impl

import com.microsoft.playwright.PlaywrightException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.function.Predicate
import java.util.regex.Pattern

class UrlMatcher private constructor(
  baseURL: URL?, @JvmField val glob: String?, @JvmField val pattern: Pattern?, val predicate: Predicate<String?>?
)
{
    private val baseURL: String?

    internal constructor(baseURL: URL?, glob: String?) : this(baseURL, glob, null, null)

    internal constructor(pattern: Pattern?) : this(null, null, pattern, null)

    internal constructor(predicate: Predicate<String?>?) : this(null, null, null, predicate)

    init
    {
        this.baseURL = baseURL?.toString()
    }

    fun test(value: String?): Boolean
    {
        return testImpl(baseURL, pattern, predicate, glob, value)
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as UrlMatcher
        if (pattern != null)
        {
            return that.pattern != null && pattern.pattern() == that.pattern.pattern() && pattern.flags() == that.pattern.flags()
        }
        if (predicate != null)
        {
            return predicate == that.predicate
        }
        if (glob != null)
        {
            return glob == that.glob
        }
        return that.pattern == null && that.predicate == null && that.glob == null
    }

    override fun hashCode(): Int
    {
        if (pattern != null)
        {
            return pattern.hashCode()
        }
        if (predicate != null)
        {
            return predicate.hashCode()
        }
        if (glob != null)
        {
            return glob.hashCode()
        }
        return 0
    }

    override fun toString(): String
    {
        if (pattern != null) return String.format(
            "<regex pattern=\"%s\" flags=\"%s\">", pattern.pattern(), Utils.toJsRegexFlags(pattern)
        )
        if (this.predicate != null) return "<predicate>"
        return String.format("<glob pattern=\"%s\">", glob)
    }

    companion object
    {
        @JvmStatic
        fun forOneOf(baseUrl: URL?, `object`: Any?): UrlMatcher
        {
            if (`object` == null)
            {
                return UrlMatcher(null, null, null, null)
            }
            if (`object` is String)
            {
                return UrlMatcher(baseUrl, `object`)
            }
            if (`object` is Pattern)
            {
                return UrlMatcher(`object`)
            }
            if (`object` is Predicate<*>)
            {
                val urlMatcher = UrlMatcher(`object` as Predicate<String?>)
                return urlMatcher
            }
            throw PlaywrightException("Url must be String, Pattern or Predicate<String>, found: " + `object`.javaClass.getTypeName())
        }

        @JvmStatic
        fun resolveUrl(baseUrl: URL, spec: String): String?
        {
            return resolveUrl(baseUrl.toString(), spec)
        }

        private fun resolveUrl(baseUrl: String?, spec: String): String?
        {
            if (baseUrl == null)
            {
                return spec
            }
            try
            {
                // Join using URI instead of URL since URL doesn't handle ws(s) protocols.
                return URI(baseUrl).resolve(spec).toString()
            } catch (e: URISyntaxException)
            {
                return spec
            }
        }

        private fun normaliseUrl(spec: String): String?
        {
            try
            {
                // Align with the Node.js URL parser which automatically adds a slash to the path if it is empty.
                val url = URI(spec)
                if (url.getScheme() != null && mutableListOf<String?>(
                        "http", "https", "ws", "wss"
                    ).contains(url.getScheme()) && url.getPath().isEmpty()
                )
                {
                    return URI(url.getScheme(), url.getAuthority(), "/", url.getQuery(), url.getFragment()).toString()
                }
                return url.toString()
            } catch (e: URISyntaxException)
            {
                return spec
            }
        }

        private fun testImpl(
            baseURL: String?, pattern: Pattern?, predicate: Predicate<String?>?, glob: String?, value: String?
        ): Boolean
        {
            var baseURL = baseURL
            var glob = glob
            if (pattern != null)
            {
                return pattern.matcher(value).find()
            }
            if (predicate != null)
            {
                return predicate.test(value)
            }
            if (glob != null)
            {
                if (!glob.startsWith("*"))
                {
                    // Allow http(s) baseURL to match ws(s) urls.
                    if (baseURL != null && Pattern.compile("^https?://").matcher(baseURL)
                            .find() && Pattern.compile("^wss?://").matcher(value).find()
                    )
                    {
                        baseURL = baseURL.replaceFirst("^http".toRegex(), "ws")
                    }
                    glob = Companion.normaliseUrl(resolveUrl(baseURL, glob)!!)
                }
                return Pattern.compile(Utils.globToRegex(glob)).matcher(value).find()
            }
            return true
        }
    }
}
