package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.impl.Serialization
import java.lang.reflect.Type
import java.util.*

class DateSerializer : JsonSerializer<Date?>
{
    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        return JsonPrimitive(Serialization.dateFormat.format(src))
    }
}
