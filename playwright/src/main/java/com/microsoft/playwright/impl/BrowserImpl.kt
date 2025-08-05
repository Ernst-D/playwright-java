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

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.microsoft.playwright.*
import com.microsoft.playwright.Browser.NewPageOptions
import com.microsoft.playwright.Browser.StartTracingOptions
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.options.HarContentPolicy
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

internal class BrowserImpl(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer), Browser
{
    @JvmField
    val contexts: MutableSet<BrowserContextImpl> = HashSet<BrowserContextImpl>()
    private val listeners = ListenerCollection<EventType?>()
    @JvmField
    var isConnectedOverWebSocket: Boolean = false
    private var isConnected = true
    @JvmField
    var browserType: BrowserTypeImpl? = null
    @JvmField
    var launchOptions: LaunchOptions? = null
    private var tracePath: Path? = null
    @JvmField
    var closeReason: String? = null

    internal enum class EventType
    {
        DISCONNECTED,
    }

    override fun onDisconnected(handler: Consumer<Browser?>?)
    {
        listeners.add(EventType.DISCONNECTED, handler)
    }

    override fun offDisconnected(handler: Consumer<Browser?>?)
    {
        listeners.remove(EventType.DISCONNECTED, handler)
    }

    override fun browserType(): BrowserType?
    {
        return browserType
    }

    override fun close(options: Browser.CloseOptions?)
    {
        withLogging("Browser.close", Runnable { closeImpl(options) })
    }

    private fun closeImpl(options: Browser.CloseOptions?)
    {
        var options = options
        if (options == null)
        {
            options = Browser.CloseOptions()
        }
        closeReason = options.reason
        if (isConnectedOverWebSocket)
        {
            try
            {
                connection!!.close()
            } catch (e: IOException)
            {
                throw PlaywrightException("Failed to close browser connection", e)
            }
            return
        }
        try
        {
            sendMessage("close")
        } catch (e: PlaywrightException)
        {
            if (!Utils.isSafeCloseError(e))
            {
                throw e
            }
        }
    }

    fun notifyRemoteClosed()
    {
        // Emulate all pages, contexts and the browser closing upon disconnect.
        for (context in ArrayList<BrowserContextImpl>(contexts))
        {
            for (page in ArrayList<PageImpl>(context.pages))
            {
                page.didClose()
            }
            context.didClose()
        }
        didClose()
    }

    override fun contexts(): MutableList<BrowserContext?>
    {
        return ArrayList<BrowserContext?>(contexts)
    }

    override fun isConnected(): Boolean
    {
        return isConnected
    }

    override fun newContext(options: Browser.NewContextOptions?): BrowserContextImpl?
    {
        return withLogging<BrowserContextImpl?>("Browser.newContext", Supplier { newContextImpl(options) })
    }

    private fun newContextImpl(options: Browser.NewContextOptions?): BrowserContextImpl
    {
        var options = options
        if (options == null)
        {
            options = Browser.NewContextOptions()
        } else
        {
            // Make a copy so that we can nullify some fields below.
            options = Utils.convertType<Browser.NewContextOptions?, Browser.NewContextOptions?>(
                options, Browser.NewContextOptions::class.java
            )
        }
        if (options.storageStatePath != null)
        {
            try
            {
                val bytes = Files.readAllBytes(options.storageStatePath)
                options.storageState = String(bytes, StandardCharsets.UTF_8)
                options.storageStatePath = null
            } catch (e: IOException)
            {
                throw PlaywrightException("Failed to read storage state from file", e)
            }
        }
        var storageState: JsonObject? = null
        if (options.storageState != null)
        {
            storageState = Gson().fromJson<JsonObject?>(options.storageState, JsonObject::class.java)
            options.storageState = null
        }
        var recordHar: JsonObject? = null
        val recordHarPath = options.recordHarPath
        var harContentPolicy: HarContentPolicy? = null
        if (options.recordHarPath != null)
        {
            recordHar = JsonObject()
            recordHar.addProperty("path", options.recordHarPath.toString())
            if (options.recordHarContent != null)
            {
                harContentPolicy = options.recordHarContent
            } else if (options.recordHarOmitContent != null && options.recordHarOmitContent)
            {
                harContentPolicy = HarContentPolicy.OMIT
            }
            if (harContentPolicy != null)
            {
                recordHar.addProperty("content", harContentPolicy.name.lowercase(Locale.getDefault()))
            }
            if (options.recordHarMode != null)
            {
                recordHar.addProperty("mode", options.recordHarMode.name.lowercase(Locale.getDefault()))
            }
            Serialization.addHarUrlFilter(recordHar, options.recordHarUrlFilter)
            options.recordHarPath = null
            options.recordHarMode = null
            options.recordHarOmitContent = null
            options.recordHarContent = null
            options.recordHarUrlFilter = null
        } else
        {
            if (options.recordHarOmitContent != null)
            {
                throw PlaywrightException("recordHarOmitContent is set but recordHarPath is null")
            }
            if (options.recordHarUrlFilter != null)
            {
                throw PlaywrightException("recordHarUrlFilter is set but recordHarPath is null")
            }
            if (options.recordHarMode != null)
            {
                throw PlaywrightException("recordHarMode is set but recordHarPath is null")
            }
            if (options.recordHarContent != null)
            {
                throw PlaywrightException("recordHarContent is set but recordHarPath is null")
            }
        }

        val params = Serialization.gson().toJsonTree(options).getAsJsonObject()
        if (storageState != null)
        {
            params.add("storageState", storageState)
        }
        if (recordHar != null)
        {
            params.add("recordHar", recordHar)
        }
        if (options.recordVideoDir != null)
        {
            val recordVideo = JsonObject()
            recordVideo.addProperty("dir", options.recordVideoDir.toAbsolutePath().toString())
            if (options.recordVideoSize != null)
            {
                recordVideo.add("size", Serialization.gson().toJsonTree(options.recordVideoSize))
            }
            params.remove("recordVideoDir")
            params.remove("recordVideoSize")
            params.add("recordVideo", recordVideo)
        } else if (options.recordVideoSize != null)
        {
            throw PlaywrightException("recordVideoSize is set but recordVideoDir is null")
        }
        if (options.viewportSize != null)
        {
            if (options.viewportSize.isPresent())
            {
                val size = params.get("viewportSize")
                params.remove("viewportSize")
                params.add("viewport", size)
            } else
            {
                params.remove("viewportSize")
                params.addProperty("noDefaultViewport", true)
            }
        }
        Utils.addToProtocol(params, options.clientCertificates)
        params.remove("acceptDownloads")
        if (options.acceptDownloads != null)
        {
            params.addProperty("acceptDownloads", if (options.acceptDownloads) "accept" else "deny")
        }
        val result = sendMessage("newContext", params)
        val context: BrowserContextImpl = connection?.getExistingObject<BrowserContextImpl?>(
            result?.getAsJsonObject()?.getAsJsonObject("context")?.get("guid")?.getAsString()
        )!!
        context.videosDir = options.recordVideoDir
        if (options.baseURL != null)
        {
            context.setBaseUrl(options.baseURL)
        }
        context.setRecordHar(recordHarPath, harContentPolicy)
        if (launchOptions != null)
        {
            context.tracing().setTracesDir(launchOptions!!.tracesDir)
        }
        contexts.add(context)
        return context
    }

    override fun newPage(options: NewPageOptions?): Page?
    {
        return withLogging<Page?>("Browser.newPage", Supplier { newPageImpl(options) })
    }

    override fun startTracing(page: Page?, options: StartTracingOptions?)
    {
        withLogging("Browser.startTracing", Runnable { startTracingImpl(page, options) })
    }

    private fun startTracingImpl(page: Page?, options: StartTracingOptions?)
    {
        var options = options
        if (options == null)
        {
            options = StartTracingOptions()
        }
        tracePath = options.path
        val params = Serialization.gson().toJsonTree(options).getAsJsonObject()
        if (page != null)
        {
            params.add("page", (page as PageImpl).toProtocolRef())
        }
        sendMessage("startTracing", params)
    }

    override fun stopTracing(): ByteArray?
    {
        return withLogging<ByteArray?>("Browser.stopTracing", Supplier { stopTracingImpl() })
    }

    private fun stopTracingImpl(): ByteArray?
    {
        val json = sendMessage("stopTracing")!!.getAsJsonObject()
        val artifact = connection!!.getExistingObject<ArtifactImpl?>(
            json.getAsJsonObject().getAsJsonObject("artifact").get("guid").getAsString()
        )
        val data = artifact!!.readAllBytes()
        artifact.delete()
        if (tracePath != null)
        {
            try
            {
                Files.createDirectories(tracePath!!.getParent())
                Files.write(tracePath, data)
            } catch (e: IOException)
            {
                throw PlaywrightException("Failed to write trace file", e)
            } finally
            {
                tracePath = null
            }
        }
        return data
    }

    private fun newPageImpl(options: NewPageOptions?): Page
    {
        val context = newContext(
            Utils.convertType<NewPageOptions?, Browser.NewContextOptions?>(
                options, Browser.NewContextOptions::class.java
            )
        )
        val page = context!!.newPage()
        page.ownedContext = context
        context.ownerPage = page
        return page
    }

    private fun name(): String?
    {
        return initializer!!.get("name").getAsString()
    }

    val isChromium: Boolean
        get() = "chromium" == name()

    override fun version(): String?
    {
        return initializer!!.get("version").getAsString()
    }

    // used to be protected, set later
    public override fun handleEvent(event: String?, parameters: JsonObject?)
    {
        if ("close" == event)
        {
            didClose()
        }
    }

    override fun newBrowserCDPSession(): CDPSession?
    {
        val params = JsonObject()
        val result = sendMessage("newBrowserCDPSession", params)!!.getAsJsonObject()
        return connection!!.getExistingObject<CDPSession?>(result.getAsJsonObject("session").get("guid").getAsString())
    }

    private fun didClose()
    {
        isConnected = false
        listeners.notify<BrowserImpl?>(EventType.DISCONNECTED, this)
    }
}
