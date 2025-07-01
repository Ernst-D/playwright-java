package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.playwright.impl.JSHandleImpl;

import java.lang.reflect.Type;

public class HandleSerializer implements JsonSerializer<JSHandleImpl> {
  @Override
  public JsonElement serialize(JSHandleImpl src, Type typeOfSrc, JsonSerializationContext context) {
    return src.toProtocolRef();
  }
}
