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
import com.microsoft.playwright.PlaywrightException
import java.util.function.Consumer

internal class ListenerCollection<EventType> @JvmOverloads constructor(
    private val eventSubscriptions: MutableMap<EventType?, String?>? = null,
    private val channelOwner: ChannelOwner? = null
)
{
    private val listeners = HashMap<EventType?, MutableList<Consumer<*>>?>()

    fun <T> notify(eventType: EventType?, param: T?)
    {
        val list = listeners.get(eventType)
        if (list == null)
        {
            return
        }

        for (listener in ArrayList<Consumer<*>>(list))
        {
            (listener as Consumer<T?>).accept(param)
        }
    }

    fun add(type: EventType?, listener: Consumer<*>?)
    {
        if (listener == null)
        {
            throw PlaywrightException("Can't add a null listener")
        }
        var list = listeners.get(type)
        if (list == null)
        {
            list = ArrayList()
            listeners.put(type, list)
            updateSubscription(type, true)
        }
        list.add(listener)
    }

    fun remove(type: EventType?, listener: Consumer<*>?)
    {
        val list = listeners.get(type)
        if (list == null)
        {
            return
        }
        list.removeAll(mutableSetOf(listener))
        if (list.isEmpty())
        {
            updateSubscription(type, false)
            listeners.remove(type)
        }
    }

    fun hasListeners(type: EventType?): Boolean
    {
        return listeners.containsKey(type)
    }

    private fun updateSubscription(eventType: EventType?, enabled: Boolean)
    {
        if (eventSubscriptions == null)
        {
            return
        }
        val protocolEvent = eventSubscriptions.get(eventType)
        if (protocolEvent == null)
        {
            return
        }
        val params = JsonObject()
        params.addProperty("event", protocolEvent)
        params.addProperty("enabled", enabled)
        channelOwner?.sendMessageAsync("updateSubscription", params)
    }
}
