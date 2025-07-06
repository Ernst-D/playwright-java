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
import com.microsoft.playwright.Tracing
import com.microsoft.playwright.Tracing.*
import java.nio.file.Path

internal class TracingImpl(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer), Tracing
{
    private var includeSources = false
    private var tracesDir: Path? = null
    private var isTracing = false
    private var stacksId: String? = null


    init
    {
        markAsInternalType()
    }

    private fun stopChunkImpl(path: Path?)
    {
        if (isTracing)
        {
            isTracing = false
            connection!!.setIsTracing(false)
        }
        val params = JsonObject()

        // Not interested in artifacts.
        if (path == null)
        {
            params.addProperty("mode", "discard")
            sendMessage("tracingStopChunk", params)
            if (stacksId != null)
            {
                connection!!.localUtils()!!.traceDiscarded(stacksId)
            }
            return
        }

        val isLocal = !connection!!.isRemote
        if (isLocal)
        {
            params.addProperty("mode", "entries")
            val json = sendMessage("tracingStopChunk", params)!!.asJsonObject
            val entries = json.getAsJsonArray("entries")
            connection.localUtils!!.zip(path, entries, stacksId, false, includeSources)
            return
        }

        params.addProperty("mode", "archive")
        val json = sendMessage("tracingStopChunk", params)!!.getAsJsonObject()
        // The artifact may be missing if the browser closed while stopping tracing.
        if (!json.has("artifact"))
        {
            if (stacksId != null)
            {
                connection.localUtils()!!.traceDiscarded(stacksId)
            }
            return
        }
        val artifact =
            connection.getExistingObject<ArtifactImpl?>(json.getAsJsonObject("artifact").get("guid").getAsString())
        artifact!!.saveAs(path)
        artifact.delete()

        connection.localUtils!!.zip(path, JsonArray(), stacksId, true, includeSources)
    }

    override fun startChunk(options: StartChunkOptions?)
    {
        var options = options
        if (options == null)
        {
            options = StartChunkOptions()
        }
        tracingStartChunk(options.name, options.title)
    }

    override fun group(name: String?, options: GroupOptions?)
    {
        withLogging("Tracing.group") {
          groupImpl(name, options)
        }
    }

    private fun groupImpl(name: String?, options: GroupOptions?)
    {
        var options = options
        if (options == null)
        {
            options = GroupOptions()
        }
        val params = Serialization.gson().toJsonTree(options).getAsJsonObject()
        params.addProperty("name", name)
        sendMessage("tracingGroup", params)
    }

    override fun groupEnd()
    {
        withLogging<JsonElement?>("Tracing.groupEnd") {
          sendMessage("tracingGroupEnd")
        }
    }

    private fun tracingStartChunk(name: String?, title: String?)
    {
        val params = JsonObject()
        if (name != null)
        {
            params.addProperty("name", name)
        }
        if (title != null)
        {
            params.addProperty("title", title)
        }
        val result = sendMessage("tracingStartChunk", params)!!.getAsJsonObject()
        startCollectingStacks(result.get("traceName").getAsString())
    }

    private fun startCollectingStacks(traceName: String?)
    {
        if (!isTracing)
        {
            isTracing = true
            connection!!.setIsTracing(true)
        }
        stacksId =
            connection!!.localUtils()!!.tracingStarted(if (tracesDir == null) null else tracesDir.toString(), traceName)
    }

    override fun start(options: StartOptions?)
    {
        var options = options
        if (options == null)
        {
            options = StartOptions()
        }
        val params = Serialization.gson().toJsonTree(options).getAsJsonObject()
        includeSources = options.sources != null && options.sources
        if (includeSources)
        {
            params.addProperty("sources", true)
        }
        sendMessage("tracingStart", params)
        tracingStartChunk(options.name, options.title)
    }

    override fun stop(options: StopOptions?)
    {
        stopChunkImpl(options?.path)
        sendMessage("tracingStop")
    }

    override fun stopChunk(options: StopChunkOptions?)
    {
        stopChunkImpl(options?.path)
    }

    fun setTracesDir(tracesDir: Path?)
    {
        this.tracesDir = tracesDir
    }
}
