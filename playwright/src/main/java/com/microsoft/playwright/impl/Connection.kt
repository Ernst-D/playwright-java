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

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.TimeoutError
import java.io.IOException
import java.lang.String
import java.time.Duration
import kotlin.Boolean
import kotlin.Int
import kotlin.Throws
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.text.contains
import kotlin.text.isEmpty
import kotlin.toString

internal class Message
{
    var id: Int = 0
    var guid: kotlin.String? = null
    var method: kotlin.String? = null
    var params: JsonObject? = null
    var result: JsonElement? = null
    var error: SerializedError? = null
    var log: JsonArray? = null

    override fun toString(): kotlin.String
    {
        return "Message{" + "id='" + id + '\'' + ", guid='" + guid + '\'' + ", method='" + method + '\'' + ", params=" + (if (params == null) null else "<...>") + ", result='" + result + '\'' + ", error='" + error + '\'' + '}'
    }
}


internal class Connection private constructor(transport: Transport, env: MutableMap<String?, String?>?, isRemote: Boolean)
{
    private val transport: Transport
    private val objects: MutableMap<kotlin.String?, ChannelOwner> = HashMap<kotlin.String?, ChannelOwner>()
    private val root: Root
    @JvmField
    val isRemote: Boolean
    private var lastId = 0
    private val stackTraceCollector: StackTraceCollector?
    private val callbacks: MutableMap<Int?, WaitableResult<JsonElement?>> =
        HashMap<Int?, WaitableResult<JsonElement?>>()
    private var apiName: kotlin.String? = null
    @JvmField
    var localUtils: LocalUtils? = null
    @JvmField
    var playwright: PlaywrightImpl? = null
    @JvmField
    val env: MutableMap<String?, String?>?
    private var tracingCount = 0

    internal inner class Root(connection: Connection?) : ChannelOwner(connection, "Root", "")
    {
        fun initialize(): PlaywrightImpl?
        {
            val params = JsonObject()
            params.addProperty("sdkLanguage", "java")
            val result = sendMessage("initialize", params.getAsJsonObject())
            return this.connection?.getExistingObject<PlaywrightImpl?>(
              result?.asJsonObject?.getAsJsonObject("playwright")?.get("guid")?.asString
            )
        }
    }

    internal constructor(pipe: Transport, env: MutableMap<String?, String?>?, localUtils: LocalUtils?) : this(
        pipe, env, true
    )
    {
        this.localUtils = localUtils
    }

    internal constructor(transport: Transport, env: MutableMap<String?, String?>?) : this(transport, env, false)

    init
    {
        var transport = transport
        this.env = env
        this.isRemote = isRemote
        if (isLogging)
        {
            transport = TransportLogger(transport)
        }
        this.transport = transport
        root = Root(this)
        stackTraceCollector = StackTraceCollector.createFromEnv(env as Map<kotlin.String?, kotlin.String?>?)
    }

    fun setIsTracing(tracing: Boolean)
    {
        if (tracing)
        {
            ++tracingCount
        } else
        {
            --tracingCount
        }
    }

    fun setApiName(name: kotlin.String?): kotlin.String?
    {
        val previous = apiName
        apiName = name
        return previous
    }

    @Throws(IOException::class)
    fun close()
    {
        transport.close()
    }

    fun sendMessage(guid: kotlin.String?, method: kotlin.String, params: JsonObject?): JsonElement?
    {
        return root.runUntil<JsonElement?>(Runnable {}, sendMessageAsync(guid, method, params))
    }

    internal fun sendMessageAsync(guid: kotlin.String?, method: kotlin.String, params: JsonObject?): WaitableResult<JsonElement?>
    {
        return internalSendMessage(guid, method, params, true)
    }

    private fun internalSendMessage(
      guid: kotlin.String?, method: kotlin.String, params: JsonObject?, sendStack: Boolean
    ): WaitableResult<JsonElement?>
    {
        val id = ++lastId
        val result = WaitableResult<JsonElement?>()
        callbacks.put(id, result)
        val message = JsonObject()
        message.addProperty("id", id)
        message.addProperty("guid", guid)
        message.addProperty("method", method)
        message.add("params", params)
        val metadata = JsonObject()
        metadata.addProperty("wallTime", System.currentTimeMillis())
        var stack: JsonArray? = null
        if (apiName == null)
        {
            metadata.addProperty("internal", true)
        } else
        {
            metadata.addProperty("apiName", apiName)
            // All but first message in an API call are considered internal and will be hidden from the inspector.
            apiName = null
            if (stackTraceCollector != null)
            {
                stack = stackTraceCollector.currentStackTrace()
                if (!stack.isEmpty())
                {
                    val location = JsonObject()
                    val frame = stack.get(0).getAsJsonObject()
                    location.addProperty("file", frame.get("file").getAsString())
                    location.addProperty("line", frame.get("line").getAsInt())
                    location.addProperty("column", frame.get("column").getAsInt())
                    metadata.add("location", location)
                }
            }
        }
        message.add("metadata", metadata)
        transport.send(message)
        if (sendStack && tracingCount > 0 && stack != null && !method.startsWith("LocalUtils"))
        {
            val callData = JsonObject()
            callData.addProperty("id", id)
            callData.add("stack", stack)
            val stackParams = JsonObject()
            stackParams.add("callData", callData)
            internalSendMessage(localUtils!!.guid, "addStackToTracingNoReply", stackParams, false)
        }
        return result
    }

    fun initializePlaywright(): PlaywrightImpl?
    {
        playwright = root.initialize()
        return playwright
    }

    fun localUtils(): LocalUtils?
    {
        return localUtils
    }

    fun <T> getExistingObject(guid: kotlin.String?): T?
    {
        val result = objects.get(guid) as T?
        if (result == null) throw PlaywrightException("Object doesn't exist: " + guid)
        return result
    }

    fun registerObject(guid: kotlin.String?, _object: ChannelOwner?)
    {
        objects.put(guid, _object!!)
    }

    fun unregisterObject(guid: kotlin.String?)
    {
        objects.remove(guid)
    }

    fun processOneMessage()
    {
        val message = transport.poll(Duration.ofMillis(10))
        if (message == null)
        {
            return
        }
        val gson = Serialization.gson()
        val messageObj = gson.fromJson<Message>(message, Message::class.java)
        dispatch(messageObj)
    }

    private fun dispatch(message: Message)
    {
//    System.out.println("Message: " + message.method + " " + message.id);
        if (message.id != 0)
        {
            val callback: WaitableResult<JsonElement?> = callbacks.get(message.id)!!
            if (callback == null)
            {
                throw PlaywrightException("Cannot find command to respond: " + message.id)
            }
            callbacks.remove(message.id)
            //      System.out.println("Message: " + message.id + " " + message);
            if (message.error == null)
            {
                callback.complete(message.result)
            } else
            {
                val callLog: kotlin.String? = formatCallLog(message.log)
                if (message.error!!.error == null)
                {
                    callback.completeExceptionally(PlaywrightException(message.error.toString() + callLog))
                } else if ("TimeoutError" == message.error!!.error!!.name)
                {
                    callback.completeExceptionally(TimeoutError(message.error!!.error.toString() + callLog))
                } else if ("TargetClosedError" == message.error!!.error!!.name)
                {
                    callback.completeExceptionally(TargetClosedError(message.error!!.error.toString() + callLog))
                } else
                {
                    callback.completeExceptionally(DriverException(message.error!!.error.toString() + callLog))
                }
            }
            return
        }

        // TODO: throw?
        if (message.method == null)
        {
            return
        }
        if (message.method == "__create__")
        {
            createRemoteObject(message.guid, message.params!!)
            return
        }

        val `object`: ChannelOwner = objects.get(message.guid)!!
        if (`object` == null)
        {
            throw PlaywrightException("Cannot find object to call " + message.method + ": " + message.guid)
        }
        if (message.method == "__adopt__")
        {
            val childGuid = message.params!!.get("guid").asString
            val child: ChannelOwner = objects.get(childGuid)!!
            if (child == null)
            {
                throw PlaywrightException("Unknown new child:  " + childGuid)
            }
            `object`.adopt(child)
            return
        }
        if (message.method == "__dispose__")
        {
            val wasCollected = message.params!!.has("reason") && "gc" == message.params!!.get("reason").getAsString()
            `object`.disposeChannelOwner(wasCollected)
            return
        }
        `object`.handleEvent(message.method as kotlin.String?, message.params)
    }

    private fun createRemoteObject(parentGuid: kotlin.String?, params: JsonObject): ChannelOwner?
    {
        val type = params.get("type").getAsString()
        val guid = params.get("guid").getAsString()

        val parent: ChannelOwner = objects.get(parentGuid)!!
        if (parent == null)
        {
            throw PlaywrightException("Cannot find parent object " + parentGuid + " to create " + guid)
        }
        val initializer = params.getAsJsonObject("initializer")
        var result: ChannelOwner? = null
        when (type)
        {
            "Android" ->
            {
            }

            "AndroidSocket" ->
            {
            }

            "AndroidDevice" ->
            {
            }

            "Artifact" -> result = ArtifactImpl(parent, type, guid, initializer)
            "BindingCall" -> result = BindingCall(parent, type, guid, initializer)
            "BrowserType" -> result = BrowserTypeImpl(parent, type, guid, initializer)
            "Browser" -> result = BrowserImpl(parent, type, guid, initializer)
            "BrowserContext" -> result = BrowserContextImpl(parent, type, guid, initializer)
            "Dialog" -> result = DialogImpl(parent, type, guid, initializer)
            "Electron" ->
            {
            }

            "ElementHandle" -> result = ElementHandleImpl(parent, type, guid, initializer)
            "APIRequestContext" ->         // Create fake object as this API is experimental an only exposed in Node.js.
                result = APIRequestContextImpl(parent, type, guid, initializer)

            "Frame" -> result = FrameImpl(parent, type, guid, initializer)
            "JSHandle" -> result = JSHandleImpl(parent, type, guid, initializer)
            "JsonPipe" -> result = JsonPipe(parent, type, guid, initializer)
            "LocalUtils" ->
            {
                result = LocalUtils(parent, type, guid, initializer)
                if (localUtils == null)
                {
                    localUtils = result
                }
            }

            "Page" -> result = PageImpl(parent, type, guid, initializer)
            "Playwright" -> result = PlaywrightImpl(parent, type, guid, initializer)
            "Request" -> result = RequestImpl(parent, type, guid, initializer)
            "Response" -> result = ResponseImpl(parent, type, guid, initializer)
            "Route" -> result = RouteImpl(parent, type, guid, initializer)
            "Stream" -> result = Stream(parent, type, guid, initializer)
            "Selectors" -> result = SelectorsImpl(parent, type, guid, initializer)
            "SocksSupport" ->
            {
            }

            "Tracing" -> result = TracingImpl(parent, type, guid, initializer)
            "WebSocket" -> result = WebSocketImpl(parent, type, guid, initializer)
            "WebSocketRoute" -> result = WebSocketRouteImpl(parent, type, guid, initializer)
            "Worker" -> result = WorkerImpl(parent, type, guid, initializer)
            "WritableStream" -> result = WritableStream(parent, type, guid, initializer)
            "CDPSession" -> result = CDPSessionImpl(parent, type, guid, initializer)
            else -> throw PlaywrightException("Unknown type " + type)
        }

        return result
    }

    companion object
    {
        private val isLogging: Boolean

        init
        {
            val debug = System.getenv("DEBUG")
            isLogging = (debug != null) && debug.contains("pw:channel")
        }

        private fun formatCallLog(log: JsonArray?): kotlin.String?
        {
            if (log == null)
            {
                return ""
            }
            var allEmpty = true
            for (e in log)
            {
                if (!e.getAsString().isEmpty())
                {
                    allEmpty = false
                    break
                }
            }
            if (allEmpty)
            {
                return ""
            }
            val lines: MutableList<kotlin.String?> = ArrayList()
            lines.add("")
            lines.add("Call log:")
            for (e in log)
            {
                lines.add("- " + e.getAsString())
            }
            lines.add("")
            return String.join("\n", lines)
        }
    }
}
