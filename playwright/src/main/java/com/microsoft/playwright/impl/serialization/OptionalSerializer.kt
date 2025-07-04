package com.microsoft.playwright.impl.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.options.*
import java.lang.reflect.Type
import java.util.*

class OptionalSerializer : JsonSerializer<Optional<*>>
{
    override fun serialize(src: Optional<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement?
    {
        assert(isSupported(typeOfSrc)) { "Unexpected optional type: " + typeOfSrc.getTypeName() }
        if (!src.isPresent())
        {
            return JsonPrimitive("no-override")
        }
        return context.serialize(src.get())
    }

    companion object
    {
        private fun isSupported(type: Type): Boolean
        {
            return object : TypeToken<Optional<Media?>?>()
            {}.getType().getTypeName() == type.getTypeName() || object : TypeToken<Optional<ColorScheme?>?>()
            {}.getType().getTypeName() == type.getTypeName() || object : TypeToken<Optional<Contrast?>?>()
            {}.getType().getTypeName() == type.getTypeName() || object : TypeToken<Optional<ForcedColors?>?>()
            {}.getType().getTypeName() == type.getTypeName() || object : TypeToken<Optional<ReducedMotion?>?>()
            {}.getType().getTypeName() == type.getTypeName() || object : TypeToken<Optional<ViewportSize?>?>()
            {}.getType().getTypeName() == type.getTypeName()
        }
    }
}
