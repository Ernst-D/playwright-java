package com.microsoft.playwright.impl.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.SameSiteAttribute;

import java.io.IOException;

public class SameSiteAdapter extends TypeAdapter<SameSiteAttribute> {
  @Override
  public void write(JsonWriter out, SameSiteAttribute value) throws IOException {
    String stringValue;
    switch (value) {
      case STRICT:
        stringValue = "Strict";
        break;
      case LAX:
        stringValue = "Lax";
        break;
      case NONE:
        stringValue = "None";
        break;
      default:
        throw new PlaywrightException("Unexpected value: " + value);
    }
    out.value(stringValue);
  }

  @Override
  public SameSiteAttribute read(JsonReader in) throws IOException {
    String value = in.nextString();
    return SameSiteAttribute.valueOf(value.toUpperCase());
  }
}
