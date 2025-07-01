package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.playwright.PlaywrightException;

import java.lang.reflect.Type;
import java.util.Map;

public class FirefoxUserPrefsSerializer implements JsonSerializer<Map<String, Object>> {
  @Override
  public JsonElement serialize(Map<String, Object> src, Type typeOfSrc, JsonSerializationContext context) {
    if (!"java.util.Map<java.lang.String, java.lang.Object>".equals(typeOfSrc.getTypeName())) {
      throw new PlaywrightException("Unexpected map type: " + typeOfSrc);
    }
    return context.serialize(src, Map.class);
  }
}
