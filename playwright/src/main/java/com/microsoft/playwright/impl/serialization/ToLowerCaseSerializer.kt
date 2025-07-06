package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.*

class ToLowerCaseSerializer<E : Enum<E>> : JsonSerializer<E?>
{
    override fun serialize(src: E?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        return JsonPrimitive(src.toString().lowercase(Locale.getDefault()))
    }
}
