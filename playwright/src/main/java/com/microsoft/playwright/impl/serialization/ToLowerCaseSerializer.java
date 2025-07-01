package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class ToLowerCaseSerializer<E extends Enum<E>> implements JsonSerializer<E> {
  @Override
  public JsonElement serialize(E src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString().toLowerCase());
  }
}
