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
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Frame
import com.microsoft.playwright.JSHandle
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.BindingCallback

internal class BindingCall(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer)
{
    private class SourceImpl(private val frame: Frame) : BindingCallback.Source
    {
        override fun context(): BrowserContext?
        {
            return page()!!.context()
        }

        override fun page(): Page?
        {
            return frame.page()
        }

        override fun frame(): Frame
        {
            return frame
        }
    }

    fun name(): String?
    {
        return initializer!!.get("name").asString
    }

    fun call(binding: BindingCallback)
    {
        try
        {
            val frame =
                connection!!.getExistingObject<Frame?>(initializer!!.getAsJsonObject("frame").get("guid").asString)
            val source: BindingCallback.Source = SourceImpl(frame!!)
            val args: MutableList<Any?> = ArrayList()
            if (initializer.has("handle"))
            {
                val handle = connection.getExistingObject<JSHandle?>(
                    initializer.getAsJsonObject("handle").get("guid").asString
                )
                args.add(handle)
            } else
            {
                for (arg in initializer.getAsJsonArray("args"))
                {
                    args.add(
                        Serialization.deserialize<Any?>(
                            Serialization.gson().fromJson(
                                arg, SerializedValue::class.java
                            )
                        )
                    )
                }
            }
            val result = binding.call(source, *args.toTypedArray())

            val params = JsonObject()
            params.add("result", Serialization.gson().toJsonTree(Serialization.serializeArgument(result)))
            sendMessage("resolve", params)
        } catch (exception: RuntimeException)
        {
            val params = JsonObject()
            params.add("error", Serialization.gson().toJsonTree(Serialization.serializeError(exception)))
            sendMessage("reject", params)
        }
    }
}
