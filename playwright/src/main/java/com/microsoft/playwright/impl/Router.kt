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

import com.google.gson.JsonObject
import com.microsoft.playwright.Route
import java.util.function.Consumer
import java.util.stream.Collectors

internal class Router
{
    private var routes: MutableList<RouteInfo?> = ArrayList<RouteInfo?>()

    private class RouteInfo(val matcher: UrlMatcher, val handler: Consumer<Route?>, var times: Int?)
    {
        fun handle(route: RouteImpl?)
        {
            handler.accept(route)
        }

        fun decrementRemainingCallCount(): Boolean
        {
            if (times == null)
            {
                return false
            }
            times = times!! - 1
            return times!! <= 0
        }
    }

    fun add(matcher: UrlMatcher, handler: Consumer<Route?>, times: Int?)
    {
        routes.add(0, RouteInfo(matcher, handler, times))
    }

    fun remove(matcher: UrlMatcher?, handler: Consumer<Route?>?)
    {
        routes = routes.stream()
            .filter { info: RouteInfo? -> info!!.matcher != matcher || (handler != null && info.handler !== handler) }
            .collect(Collectors.toList())
    }

    fun removeAll()
    {
        routes.clear()
    }

    internal enum class HandleResult
    {
        NoMatchingHandler, Handled, Fallback, PendingHandler
    }

    fun handle(route: RouteImpl): HandleResult
    {
        var result = HandleResult.NoMatchingHandler
        val it = routes.iterator()
        while (it.hasNext())
        {
            val info = it.next()
            if (!info?.matcher!!.test(route.request().url()))
            {
                continue
            }
            if (info.decrementRemainingCallCount())
            {
                it.remove()
            }
            route.fallbackCalled = false
            info.handle(route)
            if (route.isHandled())
            {
                return HandleResult.Handled
            }
            // Not immediately handled and fallback() was not called => the route
            // must be handled asynchronously.
            if (!route.fallbackCalled)
            {
                route.shouldResumeIfFallbackIsCalled = true
                return HandleResult.PendingHandler
            }
            // Fallback was called, continue to the remaining handlers.
            result = HandleResult.Fallback
        }
        return result
    }

    fun interceptionPatterns(): JsonObject
    {
        val matchers = routes.stream().map<UrlMatcher?> { r: RouteInfo? -> r!!.matcher }.collect(Collectors.toList())
        return Utils.interceptionPatterns(matchers)
    }
}
