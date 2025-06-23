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
import com.google.gson.JsonObject
import java.nio.file.Path

internal class LocalUtils(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer)
{
    init
    {
        markAsInternalType()
    }

    fun deviceDescriptors(): JsonArray?
    {
        return initializer?.getAsJsonArray("deviceDescriptors")
    }

    fun zip(zipFile: Path, entries: JsonArray?, stacksId: String?, appendMode: Boolean, includeSources: Boolean)
    {
        val params = JsonObject()
        params.addProperty("zipFile", zipFile.toString())
        params.add("entries", entries)
        params.addProperty("mode", if (appendMode) "append" else "write")
        params.addProperty("stacksId", stacksId)
        params.addProperty("includeSources", includeSources)
        sendMessage("zip", params)
    }

    fun traceDiscarded(stacksId: String?)
    {
        val params = JsonObject()
        params.addProperty("stacksId", stacksId)
        sendMessage("traceDiscarded", params)
    }

    fun tracingStarted(tracesDir: String?, traceName: String?): String?
    {
        val params = JsonObject()
        if (tracesDir != null)
        {
            params.addProperty("tracesDir", "")
        }
        params.addProperty("traceName", traceName)
        val json = connection?.localUtils()?.sendMessage("tracingStarted", params)?.asJsonObject
        return json?.get("stacksId")?.asString
    }
}
