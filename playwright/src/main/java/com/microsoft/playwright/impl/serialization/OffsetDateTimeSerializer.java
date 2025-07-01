package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Date;

import static com.microsoft.playwright.impl.Serialization.dateFormat;


public class OffsetDateTimeSerializer implements JsonSerializer<OffsetDateTime> {
  @Override
  public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(dateFormat.format(Date.from(src.toInstant())));
  }
}

