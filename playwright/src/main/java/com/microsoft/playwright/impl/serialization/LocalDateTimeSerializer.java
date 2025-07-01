package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static com.microsoft.playwright.impl.Serialization.dateFormat;

public class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
  @Override
  public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(src);
    Instant instant = src.toInstant(offset);
    return new JsonPrimitive(dateFormat.format(Date.from(instant)));
  }
}
