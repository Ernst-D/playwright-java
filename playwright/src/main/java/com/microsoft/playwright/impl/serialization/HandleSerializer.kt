package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.impl.JSHandleImpl
import java.lang.reflect.Type

class HandleSerializer : JsonSerializer<JSHandleImpl?>
{
    override fun serialize(src: JSHandleImpl?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        return src?.toProtocolRef()!!
    }
}
