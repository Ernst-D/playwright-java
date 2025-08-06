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

internal class TimeoutSettings @JvmOverloads constructor(private val parent: TimeoutSettings? = null) {
    private var defaultTimeout: Double? = null
    private var defaultNavigationTimeout: Double? = null

    fun defaultTimeout(): Double? {
        return defaultTimeout
    }

    fun defaultNavigationTimeout(): Double? {
        return defaultNavigationTimeout
    }

    fun setDefaultTimeout(timeout: Double?) {
        defaultTimeout = timeout
    }

    fun setDefaultNavigationTimeout(timeout: Double?) {
        defaultNavigationTimeout = timeout
    }

    fun timeout(timeout: Double?): Double {
        if (timeout != null) {
            return timeout
        }
        if (defaultTimeout != null) {
            return defaultTimeout!!
        }
        if (parent != null) {
            return parent.timeout(timeout)
        }
        return DEFAULT_TIMEOUT_MS.toDouble()
    }

    fun navigationTimeout(timeout: Double?): Double {
        if (timeout != null) {
            return timeout
        }
        if (defaultNavigationTimeout != null) {
            return defaultNavigationTimeout!!
        }
        if (defaultTimeout != null) {
            return defaultTimeout!!
        }
        if (parent != null) {
            return parent.navigationTimeout(timeout)
        }
        return DEFAULT_TIMEOUT_MS.toDouble()
    }

    fun <T> createWaitable(timeout: Double?): Waitable<T?> {
        if (timeout != null && timeout == 0.0) {
            return WaitableNever<T?>()
        }
        return WaitableTimeout<T?>(timeout(timeout))
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30000
    }
}
