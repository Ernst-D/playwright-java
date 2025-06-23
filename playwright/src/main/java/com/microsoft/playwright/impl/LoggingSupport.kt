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

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier

internal open class LoggingSupport
{
    fun withLogging(apiName: String?, code: Runnable)
    {
        withLogging<Any?>(apiName, Supplier {
            code.run()
            null
        })
    }

    open fun <T> withLogging(apiName: String?, code: Supplier<T?>): T?
    {
        if (isEnabled)
        {
            logApi("=> " + apiName + " started")
        }
        var success = false
        try
        {
            val result = code.get()
            success = true
            return result
        } finally
        {
            if (isEnabled)
            {
                logApi("<= " + apiName + (if (success) " succeeded" else " failed"))
            }
        }
    }

    companion object
    {
        private val isEnabled: Boolean

        init
        {
            val debug = System.getenv("DEBUG")
            isEnabled = (debug != null) && debug.contains("pw:api")
        }

        private val timestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        ).withZone(ZoneId.of("UTC"))

        @JvmStatic
        fun logWithTimestamp(message: String?)
        {
            // This matches log format produced by the server.
            val timestamp = ZonedDateTime.now().format(timestampFormat)
            System.err.println(timestamp + " " + message)
        }

        @JvmStatic
        fun logApiIfEnabled(message: String?)
        {
            if (isEnabled)
            {
                logApi(message)
            }
        }

        fun logApi(message: String?)
        {
            // This matches log format produced by the server.
            logWithTimestamp("pw:api " + message)
        }
    }
}
