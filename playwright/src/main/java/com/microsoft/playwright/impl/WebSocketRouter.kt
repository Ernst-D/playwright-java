package com.microsoft.playwright.impl

import com.google.gson.JsonObject
import com.microsoft.playwright.WebSocketRoute
import java.util.function.Consumer
import java.util.stream.Collectors

internal class WebSocketRouter
{
    private val routes: MutableList<RouteInfo> = ArrayList<RouteInfo>()

    private class RouteInfo(val matcher: UrlMatcher, private val handler: Consumer<WebSocketRoute?>)
    {
        fun handle(route: WebSocketRouteImpl?)
        {
            handler.accept(route)
            route?.afterHandle()
        }
    }

    fun add(matcher: UrlMatcher, handler: Consumer<WebSocketRoute?>)
    {
        routes.add(0, RouteInfo(matcher, handler))
    }

    fun handle(route: WebSocketRouteImpl?): Boolean
    {
        for (routeInfo in routes)
        {
            if (routeInfo.matcher.test(route?.url()))
            {
                routeInfo.handle(route)
                return true
            }
        }
        return false
    }

    fun interceptionPatterns(): JsonObject
    {
        val matchers = routes.stream().map<UrlMatcher?> { r: RouteInfo? -> r!!.matcher }.collect(Collectors.toList())
        return Utils.interceptionPatterns(matchers)
    }
}
