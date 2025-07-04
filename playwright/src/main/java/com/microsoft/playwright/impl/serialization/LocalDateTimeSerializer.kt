package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.microsoft.playwright.impl.Serialization
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime?>
{
    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        val offset = ZoneId.systemDefault().getRules().getOffset(src)
        val instant = src?.toInstant(offset)!!
        return JsonPrimitive(Serialization.dateFormat.format(Date.from(instant)))
    }
}
