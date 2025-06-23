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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.microsoft.playwright.PlaywrightException
import java.util.function.Function
import java.util.function.Supplier

internal open class ChannelOwner private constructor(
  @JvmField val connection: Connection?,
  private var parent: ChannelOwner?,
  val type: String?,
  @JvmField val guid: String?,
  @JvmField val initializer: JsonObject?
) : LoggingSupport()
{
    private val objects: MutableMap<String?, ChannelOwner> = HashMap<String?, ChannelOwner>()

    private var wasCollected = false
    private var isInternalType = false

    constructor(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) : this(
        parent?.connection, parent, type, guid, initializer
    )

    constructor(connection: Connection?, type: String?, guid: String?) : this(connection, null, type, guid, JsonObject())


    init
    {
        connection?.registerObject(guid, this)
        if (parent != null)
        {
            parent!!.objects.put(guid, this)
        }
    }

    fun markAsInternalType()
    {
        isInternalType = true
    }

    fun disposeChannelOwner(wasGarbageCollected: Boolean)
    {
        // Clean up from parent and connection.
        if (parent != null)
        {
            parent!!.objects.remove(guid)
        }
        connection?.unregisterObject(guid)
        wasCollected = wasGarbageCollected
        // Dispose all children.
        for (child in ArrayList<ChannelOwner>(objects.values))
        {
            child.disposeChannelOwner(wasGarbageCollected)
        }
        objects.clear()
    }

    fun adopt(child: ChannelOwner)
    {
        child.parent!!.objects.remove(child.guid)
        objects.put(child.guid, child)
        child.parent = this
    }

    fun <T> withWaitLogging(apiName: String?, code: Function<Logger?, T?>?): T?
    {
        return WaitForEventLogger<T?>(this, apiName, code).get()
    }

    override fun <T> withLogging(apiName: String?, code: Supplier<T?>): T?
    {
        var apiName = apiName
        if (isInternalType)
        {
            apiName = null
        }
        val previousApiName = connection?.setApiName(apiName)
        try
        {
            return super.withLogging<T?>(apiName, code)
        } finally
        {
            connection?.setApiName(previousApiName)
        }
    }

    fun sendMessageAsync(method: String): WaitableResult<JsonElement?>
    {
        return sendMessageAsync(method, JsonObject())
    }

    fun sendMessageAsync(method: String, params: JsonObject?): WaitableResult<JsonElement?>
    {
        checkNotCollected()
        return connection!!.sendMessageAsync(guid, method, params)
    }

    fun sendMessage(method: String): JsonElement?
    {
        return sendMessage(method, JsonObject())
    }

    fun sendMessage(method: String, params: JsonObject?): JsonElement?
    {
        checkNotCollected()
        return connection?.sendMessage(guid, method, params)
    }

    private fun checkNotCollected()
    {
        if (wasCollected) throw PlaywrightException("The object has been collected to prevent unbounded heap growth.")
    }

    fun <T> runUntil(code: Runnable, waitable: Waitable<T?>): T?
    {
        try
        {
            code.run()
            while (!waitable.isDone())
            {
                connection?.processOneMessage()
            }
            return waitable.get()
        } finally
        {
            waitable.dispose()
        }
    }

    open fun handleEvent(event: String?, parameters: JsonObject?)
    {
    }

    fun toProtocolRef(): JsonObject
    {
        val json = JsonObject()
        json.addProperty("guid", guid)
        return json
    }
}
