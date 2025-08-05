package com.microsoft.playwright.impl

import com.google.gson.JsonObject
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.WebSocketFrame
import com.microsoft.playwright.WebSocketRoute
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

internal class WebSocketRouteImpl(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer), WebSocketRoute
{
    private var onPageMessage: Consumer<WebSocketFrame?>? = null
    private var onPageClose: BiConsumer<Int?, String?>? = null
    private var onServerMessage: Consumer<WebSocketFrame?>? = null
    private var onServerClose: BiConsumer<Int?, String?>? = null
    private var connected = false
    private val server: WebSocketRoute = object : WebSocketRoute
    {
        override fun close(options: WebSocketRoute.CloseOptions?)
        {
            var options = options
            if (options == null)
            {
                options = WebSocketRoute.CloseOptions()
            }
            val params = Serialization.gson().toJsonTree(options).asJsonObject
            params.addProperty("wasClean", true)
            sendMessageAsync("closeServer", params)
        }

        override fun connectToServer(): WebSocketRoute?
        {
            throw PlaywrightException("connectToServer must be called on the page-side WebSocketRoute")
        }

        override fun onClose(handler: BiConsumer<Int?, String?>?)
        {
            onServerClose = handler
        }

        override fun onMessage(handler: Consumer<WebSocketFrame?>?)
        {
            onServerMessage = handler
        }

        override fun send(message: String?)
        {
            val params = JsonObject()
            params.addProperty("message", message)
            params.addProperty("isBase64", false)
            sendMessageAsync("sendToServer", params)
        }

        override fun send(message: ByteArray?)
        {
            val params = JsonObject()
            val base64 = Base64.getEncoder().encodeToString(message)
            params.addProperty("message", base64)
            params.addProperty("isBase64", true)
            sendMessageAsync("sendToServer", params)
        }

        override fun url(): String?
        {
            return initializer!!.get("url").asString
        }
    }

    init
    {
        markAsInternalType()
    }

    override fun close(options: WebSocketRoute.CloseOptions?)
    {
        var options = options
        if (options == null)
        {
            options = WebSocketRoute.CloseOptions()
        }
        val params = Serialization.gson().toJsonTree(options).asJsonObject
        params.addProperty("wasClean", true)
        sendMessageAsync("closePage", params)
    }

    override fun connectToServer(): WebSocketRoute
    {
        if (connected)
        {
            throw PlaywrightException("Already connected to the server")
        }
        connected = true
        sendMessageAsync("connect")
        return server
    }

    override fun onClose(handler: BiConsumer<Int?, String?>?)
    {
        onPageClose = handler
    }

    override fun onMessage(handler: Consumer<WebSocketFrame?>?)
    {
        onPageMessage = handler
    }

    override fun send(message: String?)
    {
        val params = JsonObject()
        params.addProperty("message", message)
        params.addProperty("isBase64", false)
        sendMessageAsync("sendToPage", params)
    }

    override fun send(message: ByteArray?)
    {
        val params = JsonObject()
        val base64 = Base64.getEncoder().encodeToString(message)
        params.addProperty("message", base64)
        params.addProperty("isBase64", true)
        sendMessageAsync("sendToPage", params)
    }

    override fun url(): String?
    {
        return initializer?.get("url")?.asString
    }

    fun afterHandle()
    {
        if (this.connected)
        {
            return
        }
        // Ensure that websocket is "open" and can send messages without an actual server connection.
        sendMessageAsync("ensureOpened")
    }

    // used to protected
    override fun handleEvent(event: String?, parameters: JsonObject?)
    {
        if ("messageFromPage" == event)
        {
            val message = parameters?.get("message")?.asString
            val isBase64 = parameters?.get("isBase64")?.asBoolean
            if (onPageMessage != null)
            {
                onPageMessage!!.accept(WebSocketFrameImpl(message, isBase64 == true))
            } else if (connected)
            {
                val messageParams = JsonObject()
                messageParams.addProperty("message", message)
                messageParams.addProperty("isBase64", isBase64)
                sendMessageAsync("sendToServer", messageParams)
            }
        } else if ("messageFromServer" == event)
        {
            val message = parameters?.get("message")?.asString
            val isBase64 = parameters?.get("isBase64")?.asBoolean
            if (onServerMessage != null)
            {
                onServerMessage!!.accept(WebSocketFrameImpl(message, isBase64 == true))
            } else
            {
                val messageParams = JsonObject()
                messageParams.addProperty("message", message)
                messageParams.addProperty("isBase64", isBase64)
                sendMessageAsync("sendToPage", messageParams)
            }
        } else if ("closePage" == event)
        {
            val code = parameters?.get("code")?.asInt
            val reason = parameters?.get("reason")?.asString
            val wasClean = parameters?.get("wasClean")?.asBoolean
            if (onPageClose != null)
            {
                onPageClose!!.accept(code, reason)
            } else
            {
                val closeParams = JsonObject()
                closeParams.addProperty("code", code)
                closeParams.addProperty("reason", reason)
                closeParams.addProperty("wasClean", wasClean)
                sendMessageAsync("closeServer", closeParams)
            }
        } else if ("closeServer" == event)
        {
            val code = parameters?.get("code")?.asInt
            val reason = parameters?.get("reason")?.asString
            val wasClean = parameters?.get("wasClean")?.asBoolean
            if (onServerClose != null)
            {
                onServerClose!!.accept(code, reason)
            } else
            {
                val closeParams = JsonObject()
                closeParams.addProperty("code", code)
                closeParams.addProperty("reason", reason)
                closeParams.addProperty("wasClean", wasClean)
                sendMessageAsync("closePage", closeParams)
            }
        }
    }
}
