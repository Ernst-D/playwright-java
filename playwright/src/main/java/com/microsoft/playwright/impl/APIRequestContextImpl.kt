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
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.microsoft.playwright.APIRequestContext
import com.microsoft.playwright.APIRequestContext.DisposeOptions
import com.microsoft.playwright.APIResponse
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.Request
import com.microsoft.playwright.options.FilePayload
import com.microsoft.playwright.options.RequestOptions
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier

internal class APIRequestContextImpl(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject) :
    ChannelOwner(parent, type, guid, initializer), APIRequestContext
{
    private val tracing: TracingImpl? = connection!!.getExistingObject<TracingImpl?>(
        initializer.getAsJsonObject("tracing").get("guid").getAsString()
    )
  private var disposeReason: String? = null

  override fun delete(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "DELETE"))
    }

    override fun dispose(options: DisposeOptions?)
    {
        withLogging("APIRequestContext.dispose", Runnable { disposeImpl(options) })
    }

    private fun disposeImpl(options: DisposeOptions?)
    {
        var options = options
        if (options == null)
        {
            options = DisposeOptions()
        }
        disposeReason = options.reason
        val params = Serialization.gson().toJsonTree(options).getAsJsonObject()
        sendMessage("dispose", params)
    }

    override fun fetch(urlOrRequest: String?, options: RequestOptions?): APIResponse?
    {
        return withLogging<APIResponse?>(
            "APIRequestContext.fetch", Supplier { fetchImpl(urlOrRequest, options as RequestOptionsImpl?) })
    }

    override fun fetch(request: Request, optionsArg: RequestOptions?): APIResponse?
    {
        var options = optionsArg as RequestOptionsImpl?
        if (options == null)
        {
            options = RequestOptionsImpl()
        }
        if (options.method == null)
        {
            options.method = request.method()
        }
        if (options.headers == null)
        {
            options.headers = request.headers()
        }
        if (options.data == null && options.form == null && options.multipart == null)
        {
            options.data = request.postDataBuffer()
        }
        return fetch(request.url(), options)
    }

    private fun fetchImpl(url: String?, options: RequestOptionsImpl?): APIResponse
    {
        var options = options
        if (disposeReason != null)
        {
            throw PlaywrightException(disposeReason)
        }
        if (options == null)
        {
            options = RequestOptionsImpl()
        }
        val params = JsonObject()
        params.addProperty("url", url)
        if (options.params != null)
        {
            val queryParams: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
            for (e in options.params.entries)
            {
                queryParams.put(e.key, "" + e.value)
            }
            params.add("params", Serialization.toNameValueArray(queryParams.entries))
        }
        if (options.method != null)
        {
            params.addProperty("method", options.method)
        }
        if (options.headers != null)
        {
            params.add("headers", Serialization.toProtocol(options.headers))
        }

        if (options.data != null)
        {
            var bytes: ByteArray? = null
            if (options.data is ByteArray)
            {
                bytes = options.data as ByteArray
            } else if (options.data is String)
            {
                val stringData = options.data as String
                if (!isJsonContentType(options.headers) || isJsonParsable(stringData))
                {
                    bytes = (stringData).toByteArray(StandardCharsets.UTF_8)
                }
            }
            if (bytes == null)
            {
                params.addProperty("jsonData", Serialization.jsonDataSerializer.toJson(options.data))
            } else
            {
                val base64 = Base64.getEncoder().encodeToString(bytes)
                params.addProperty("postData", base64)
            }
        }
        if (options.form != null)
        {
            params.add("formData", Serialization.toNameValueArray(options.form.fields))
        }
        if (options.multipart != null)
        {
            params.add("multipartData", serializeMultipartData(options.multipart.fields))
        }
        if (options.timeout != null)
        {
            params.addProperty("timeout", options.timeout)
        }
        if (options.failOnStatusCode != null)
        {
            params.addProperty("failOnStatusCode", options.failOnStatusCode)
        }
        if (options.ignoreHTTPSErrors != null)
        {
            params.addProperty("ignoreHTTPSErrors", options.ignoreHTTPSErrors)
        }
        if (options.maxRedirects != null)
        {
            if (options.maxRedirects < 0)
            {
                throw PlaywrightException("'maxRedirects' should be greater than or equal to '0'")
            }
            params.addProperty("maxRedirects", options.maxRedirects)
        }
        if (options.maxRetries != null)
        {
            if (options.maxRetries < 0)
            {
                throw PlaywrightException("'maxRetries' must be greater than or equal to '0'")
            }
            params.addProperty("maxRetries", options.maxRetries)
        }
        val json = sendMessage("fetch", params)!!.getAsJsonObject()
        return APIResponseImpl(this, json.getAsJsonObject("response"))
    }

    override fun get(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "GET"))
    }

    override fun head(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "HEAD"))
    }

    override fun patch(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "PATCH"))
    }

    override fun post(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "POST"))
    }

    override fun put(url: String?, options: RequestOptions?): APIResponse?
    {
        return fetch(url, ensureOptions(options, "PUT"))
    }

    override fun storageState(options: APIRequestContext.StorageStateOptions?): String?
    {
        return withLogging<String?>("APIRequestContext.storageState", Supplier {
            val json = sendMessage("storageState")
            val storageState = json.toString()
            if (options != null && options.path != null)
            {
                Utils.writeToFile(storageState.toByteArray(StandardCharsets.UTF_8), options.path)
            }
            storageState
        })
    }

    companion object
    {
        private fun isJsonContentType(headers: MutableMap<String?, String?>?): Boolean
        {
            if (headers == null)
            {
                return false
            }
            for (e in headers.entries)
            {
                if ("content-type".equals(e.key, ignoreCase = true))
                {
                    return "application/json" == e.value
                }
            }
            return false
        }

        private fun serializeMultipartData(data: MutableList<out MutableMap.MutableEntry<String?, Any?>>): JsonArray
        {
            val result = JsonArray()
            for (e in data)
            {
                var filePayload: FilePayload? = null
                if (e.value is FilePayload)
                {
                    filePayload = e.value as FilePayload
                } else if (e.value is Path)
                {
                    filePayload = Utils.toFilePayload(e.value as Path)
                } else if (e.value is File)
                {
                    filePayload = Utils.toFilePayload((e.value as File).toPath())
                }
                val item = JsonObject()
                item.addProperty("name", e.key)
                if (filePayload == null)
                {
                    item.addProperty("value", "" + e.value)
                } else
                {
                    item.add("file", Serialization.toProtocol(filePayload))
                }
                result.add(item)
            }
            return result
        }

        private fun ensureOptions(options: RequestOptions?, method: String?): RequestOptionsImpl
        {
            var impl = Utils.clone<RequestOptionsImpl?>(options as RequestOptionsImpl?)
            if (impl == null)
            {
                impl = RequestOptionsImpl()
            }
            if (impl.method == null)
            {
                impl.method = method
            }
            return impl
        }

        private fun isJsonParsable(value: String): Boolean
        {
            try
            {
                val result = JsonParser.parseString(value)
                if (result != null && result.isJsonPrimitive())
                {
                    val primitive = result.getAsJsonPrimitive()
                    if (primitive.isString() && value == primitive.getAsString())
                    {
                        // Gson parses unquoted strings too, but we don't want to treat them
                        // as valid JSON.
                        return false
                    }
                }
                return true
            } catch (error: JsonSyntaxException)
            {
                return false
            }
        }
    }
}
