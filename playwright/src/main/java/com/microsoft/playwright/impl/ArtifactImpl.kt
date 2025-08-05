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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Path

internal class ArtifactImpl(parent: ChannelOwner?, type: String?, guid: String?, initializer: JsonObject?) :
    ChannelOwner(parent, type, guid, initializer)
{
    fun createReadStream(): InputStream
    {
        val result = sendMessage("stream")!!.asJsonObject
        val stream = connection!!.getExistingObject<Stream?>(result.getAsJsonObject("stream").get("guid").asString)
        return stream!!.stream()
    }

    fun readAllBytes(): ByteArray?
    {
        val bufLen = 1024 * 1024
        val buf = ByteArray(bufLen)
        var readLen: Int
        try
        {
            ByteArrayOutputStream().use { outputStream ->
                createReadStream().use { stream ->
                    while ((stream.read(buf, 0, bufLen).also { readLen = it }) != -1)
                    {
                        outputStream.write(buf, 0, readLen)
                    }
                    return outputStream.toByteArray()
                }
            }
        } catch (e: IOException)
        {
            throw PlaywrightException("Failed to read artifact", e)
        }
    }

    fun cancel()
    {
        sendMessage("cancel")
    }

    fun delete()
    {
        sendMessage("delete")
    }

    fun failure(): String?
    {
        val result = sendMessage("failure")!!.asJsonObject
        if (result.has("error"))
        {
            return result.get("error").asString
        }
        return null
    }

    fun pathAfterFinished(): Path
    {
        if (connection!!.isRemote)
        {
            throw PlaywrightException("Path is not available when using browserType.connect(). Use download.saveAs() to save a local copy.")
        }
        val json = sendMessage("pathAfterFinished")!!.asJsonObject
        return FileSystems.getDefault().getPath(json.get("value").asString)
    }

    fun saveAs(path: Path)
    {
        if (connection!!.isRemote)
        {
            val jsonObject = sendMessage("saveAsStream")!!.asJsonObject
            val stream =
                connection.getExistingObject<Stream?>(jsonObject.getAsJsonObject("stream").get("guid").asString)
            Utils.writeToFile(stream!!.stream(), path)
            return
        }

        val params = JsonObject()
        params.addProperty("path", path.toString())
        sendMessage("saveAs", params)
    }
}
