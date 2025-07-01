package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.PlaywrightException
import java.lang.reflect.Type

class FirefoxUserPrefsSerializer : JsonSerializer<MutableMap<String?, Any?>?>
{
    override fun serialize(
        src: MutableMap<String?, Any?>?, typeOfSrc: Type, context: JsonSerializationContext
    ): JsonElement?
    {
        if ("java.util.Map<java.lang.String, java.lang.Object>" != typeOfSrc.typeName)
        {
            throw PlaywrightException("Unexpected map type: $typeOfSrc")
        }
        return context.serialize(src, MutableMap::class.java)
    }
}
