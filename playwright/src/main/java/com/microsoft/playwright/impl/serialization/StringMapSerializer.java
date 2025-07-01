 package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.playwright.PlaywrightException;

import java.lang.reflect.Type;
import java.util.Map;

import static com.microsoft.playwright.impl.Serialization.toProtocol;

public class StringMapSerializer implements JsonSerializer<Map<String, String>> {
  @Override
  public JsonElement serialize(Map<String, String> src, Type typeOfSrc, JsonSerializationContext context) {
    if (!"java.util.Map<java.lang.String, java.lang.String>".equals(typeOfSrc.getTypeName())) {
      throw new PlaywrightException("Unexpected map type: " + typeOfSrc);
    }
    return toProtocol(src);
  }
}
