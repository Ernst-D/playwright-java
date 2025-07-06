package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.impl.Serialization
import java.lang.reflect.Type

class StringMapSerializer : JsonSerializer<MutableMap<String?, String?>?>
{
    override fun serialize(
        src: MutableMap<String?, String?>?, typeOfSrc: Type, context: JsonSerializationContext?
    ): JsonElement
    {
        if ("java.util.Map<java.lang.String, java.lang.String>" != typeOfSrc.getTypeName())
        {
            throw PlaywrightException("Unexpected map type: $typeOfSrc")
        }
        return Serialization.toProtocol(src)
    }
}
