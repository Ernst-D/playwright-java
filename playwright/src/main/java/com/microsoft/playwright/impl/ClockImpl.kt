package com.microsoft.playwright.impl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.microsoft.playwright.Clock
import com.microsoft.playwright.Clock.InstallOptions
import java.util.*
import java.util.function.Supplier

internal class ClockImpl(browserContext: BrowserContextImpl) : Clock
{
    private val browserContext: ChannelOwner

    init
    {
        this.browserContext = browserContext
    }

    private fun sendMessageWithLogging(method: String, params: JsonObject?)
    {
        val capitalizedMethod = method.substring(0, 1).uppercase(Locale.getDefault()) + method.substring(1)
        browserContext.withLogging<JsonElement?>(
            "Clock." + method, Supplier {
                browserContext.sendMessage(
                    "clock" + capitalizedMethod, params
                )
            })
    }

    override fun fastForward(ticks: Long)
    {
        val params = JsonObject()
        params.addProperty("ticksNumber", ticks)
        sendMessageWithLogging("fastForward", params)
    }

    override fun fastForward(ticks: String?)
    {
        val params = JsonObject()
        params.addProperty("ticksString", ticks)
        sendMessageWithLogging("fastForward", params)
    }

    override fun install(options: InstallOptions?)
    {
        val params = JsonObject()
        if (options != null)
        {
            parseTime(options.time, params)
        }
        sendMessageWithLogging("install", params)
    }

    override fun runFor(ticks: Long)
    {
        val params = JsonObject()
        params.addProperty("ticksNumber", ticks)
        sendMessageWithLogging("runFor", params)
    }

    override fun runFor(ticks: String?)
    {
        val params = JsonObject()
        params.addProperty("ticksString", ticks)
        sendMessageWithLogging("runFor", params)
    }

    override fun pauseAt(time: Long)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time)
        sendMessageWithLogging("pauseAt", params)
    }

    override fun pauseAt(time: String?)
    {
        val params = JsonObject()
        params.addProperty("timeString", time)
        sendMessageWithLogging("pauseAt", params)
    }

    override fun pauseAt(time: Date)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time.getTime())
        sendMessageWithLogging("pauseAt", params)
    }

    override fun resume()
    {
        sendMessageWithLogging("resume", JsonObject())
    }

    override fun setFixedTime(time: Long)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time)
        sendMessageWithLogging("setFixedTime", params)
    }

    override fun setFixedTime(time: String?)
    {
        val params = JsonObject()
        params.addProperty("timeString", time)
        sendMessageWithLogging("setFixedTime", params)
    }

    override fun setFixedTime(time: Date)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time.getTime())
        sendMessageWithLogging("setFixedTime", params)
    }

    override fun setSystemTime(time: Long)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time)
        sendMessageWithLogging("setSystemTime", params)
    }

    override fun setSystemTime(time: String?)
    {
        val params = JsonObject()
        params.addProperty("timeString", time)
        sendMessageWithLogging("setSystemTime", params)
    }

    override fun setSystemTime(time: Date)
    {
        val params = JsonObject()
        params.addProperty("timeNumber", time.getTime())
        sendMessageWithLogging("setSystemTime", params)
    }

    companion object
    {
        private fun parseTime(time: Any?, params: JsonObject)
        {
            if (time is Long)
            {
                params.addProperty("timeNumber", time)
            } else if (time is Date)
            {
                params.addProperty("timeNumber", time.getTime())
            } else if (time is String)
            {
                params.addProperty("timeString", time)
            }
        }
    }
}
