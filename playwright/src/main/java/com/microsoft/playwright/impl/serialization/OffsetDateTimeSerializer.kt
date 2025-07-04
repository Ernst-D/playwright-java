package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.impl.Serialization
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.util.*

class OffsetDateTimeSerializer : JsonSerializer<OffsetDateTime?>
{
    override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        return JsonPrimitive(Serialization.dateFormat.format(Date.from(src?.toInstant()!!)))
    }
}

