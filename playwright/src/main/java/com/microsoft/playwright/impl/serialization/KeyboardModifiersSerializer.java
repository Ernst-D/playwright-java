package com.microsoft.playwright.impl.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.playwright.options.KeyboardModifier;

import java.lang.reflect.Type;
import java.util.List;

// part of serialization.java, move to single object later
public class KeyboardModifiersSerializer implements JsonSerializer<List<KeyboardModifier>> {
  @Override
  public JsonArray serialize(List<KeyboardModifier> modifiers, Type typeOfSrc, JsonSerializationContext context) {
    JsonArray result = new JsonArray();
    if (modifiers.contains(KeyboardModifier.ALT)) {
      result.add("Alt");
    }
    if (modifiers.contains(KeyboardModifier.CONTROL)) {
      result.add("Control");
    }
    if (modifiers.contains(KeyboardModifier.CONTROLORMETA)) {
      result.add("ControlOrMeta");
    }
    if (modifiers.contains(KeyboardModifier.META)) {
      result.add("Meta");
    }
    if (modifiers.contains(KeyboardModifier.SHIFT)) {
      result.add("Shift");
    }
    return result;
  }
}
