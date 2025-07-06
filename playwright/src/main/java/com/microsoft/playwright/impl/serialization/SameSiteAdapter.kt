package com.microsoft.playwright.impl.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.options.SameSiteAttribute
import java.io.IOException
import java.util.*

class SameSiteAdapter : TypeAdapter<SameSiteAttribute?>()
{
  @Throws(IOException::class)
  override fun write(out: JsonWriter?, value: SameSiteAttribute?)
  {
    val stringValue = when (value)
    {
      SameSiteAttribute.STRICT -> "Strict"
      SameSiteAttribute.LAX -> "Lax"
      SameSiteAttribute.NONE -> "None"
      else -> throw PlaywrightException("Unexpected value: " + value)
    }
    out?.value(stringValue)
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): SameSiteAttribute
  {
    val value = `in`.nextString()
    return SameSiteAttribute.valueOf(value.uppercase(Locale.getDefault()))
  }
}
