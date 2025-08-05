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
import com.microsoft.playwright.CDPSession
import java.util.function.Consumer

internal class CDPSessionImpl(
    parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?
) : ChannelOwner(parent, type, guid, initializer), CDPSession
{
    private val listeners = ListenerCollection<String?>(HashMap(), this)

    // used to be protected, set later
    override fun handleEvent(event: String?, parameters: JsonObject?)
    {
        super.handleEvent(event, parameters)
        if ("event" == event)
        {
            val method = parameters?.get("method")?.asString
            var params: JsonObject? = null
            if (parameters?.has("params")!!)
            {
                params = parameters.get("params").asJsonObject
            }
            listeners.notify<JsonObject?>(method, params)
        }
    }

    override fun send(method: String?): JsonObject?
    {
        return send(method, null)
    }

    override fun send(method: String?, params: JsonObject?): JsonObject?
    {
        val args = JsonObject()
        if (params != null)
        {
            args.add("params", params)
        }
        args.addProperty("method", method)
        val response = connection!!.sendMessage(guid, "send", args)
        if (response == null) return null
        else return response.asJsonObject.get("result").asJsonObject
    }

    override fun on(event: String?, handler: Consumer<JsonObject?>)
    {
        listeners.add(event, handler)
    }

    override fun off(event: String?, handler: Consumer<JsonObject?>?)
    {
        listeners.remove(event, handler)
    }

    override fun detach()
    {
        sendMessage("detach")
    }
}
