package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.options.*;

import java.lang.reflect.Type;
import java.util.Optional;

public class OptionalSerializer implements JsonSerializer<Optional<?>> {
  private static boolean isSupported(Type type) {
    return new TypeToken<Optional<Media>>() {}.getType().getTypeName().equals(type.getTypeName()) ||
      new TypeToken<Optional<ColorScheme>>() {}.getType().getTypeName().equals(type.getTypeName()) ||
      new TypeToken<Optional<Contrast>>() {}.getType().getTypeName().equals(type.getTypeName()) ||
      new TypeToken<Optional<ForcedColors>>() {}.getType().getTypeName().equals(type.getTypeName()) ||
      new TypeToken<Optional<ReducedMotion>>() {}.getType().getTypeName().equals(type.getTypeName()) ||
      new TypeToken<Optional<ViewportSize>>() {}.getType().getTypeName().equals(type.getTypeName());
  }

  @Override
  public JsonElement serialize(Optional<?> src, Type typeOfSrc, JsonSerializationContext context) {
    assert isSupported(typeOfSrc) : "Unexpected optional type: " + typeOfSrc.getTypeName();
    if (!src.isPresent()) {
      return new JsonPrimitive("no-override");
    }
    return context.serialize(src.get());
  }
}
