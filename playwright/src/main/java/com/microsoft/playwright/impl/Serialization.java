/*
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.playwright.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.impl.serialization.*;
import com.microsoft.playwright.options.*;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

import static com.microsoft.playwright.impl.Utils.toJsRegexFlags;
import static com.microsoft.playwright.impl.Utils.fromJsRegexFlags;

public class Serialization {
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping()
    .registerTypeAdapter(Date.class, new DateSerializer())
    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
    .registerTypeAdapter(SameSiteAttribute.class, new SameSiteAdapter().nullSafe())
    .registerTypeAdapter(BrowserChannel.class, new ToLowerCaseAndDashSerializer<BrowserChannel>())
    .registerTypeAdapter(ColorScheme.class, new ToLowerCaseAndDashSerializer<ColorScheme>())
    .registerTypeAdapter(Contrast.class, new ToLowerCaseAndDashSerializer<Contrast>())
    .registerTypeAdapter(Media.class, new ToLowerCaseSerializer<Media>())
    .registerTypeAdapter(ForcedColors.class, new ToLowerCaseSerializer<ForcedColors>())
    .registerTypeAdapter(HttpCredentialsSend.class, new ToLowerCaseSerializer<HttpCredentialsSend>())
    .registerTypeAdapter(ReducedMotion.class, new ToLowerCaseAndDashSerializer<ReducedMotion>())
    .registerTypeAdapter(ScreenshotAnimations.class, new ToLowerCaseSerializer<ScreenshotAnimations>())
    .registerTypeAdapter(ScreenshotType.class, new ToLowerCaseSerializer<ScreenshotType>())
    .registerTypeAdapter(ScreenshotScale.class, new ToLowerCaseSerializer<ScreenshotScale>())
    .registerTypeAdapter(ScreenshotCaret.class, new ToLowerCaseSerializer<ScreenshotCaret>())
    .registerTypeAdapter(ServiceWorkerPolicy.class, new ToLowerCaseAndDashSerializer<ServiceWorkerPolicy>())
    .registerTypeAdapter(MouseButton.class, new ToLowerCaseSerializer<MouseButton>())
    .registerTypeAdapter(LoadState.class, new ToLowerCaseSerializer<LoadState>())
    .registerTypeAdapter(WaitUntilState.class, new ToLowerCaseSerializer<WaitUntilState>())
    .registerTypeAdapter(WaitForSelectorState.class, new ToLowerCaseSerializer<WaitForSelectorState>())
    .registerTypeAdapter((new TypeToken<List<KeyboardModifier>>(){}).getType(), new KeyboardModifiersSerializer())
    .registerTypeAdapter(Optional.class, new OptionalSerializer())
    .registerTypeHierarchyAdapter(JSHandleImpl.class, new HandleSerializer())
    .registerTypeAdapter((new TypeToken<Map<String, String>>(){}).getType(), new StringMapSerializer())
    .registerTypeAdapter((new TypeToken<Map<String, Object>>(){}).getType(), new FirefoxUserPrefsSerializer())
    .registerTypeHierarchyAdapter(Path.class, new PathSerializer()).create();

  static Gson gson() {
    return gson;
  }

  static final Gson jsonDataSerializer = new GsonBuilder().disableHtmlEscaping()
    .registerTypeAdapter(Date.class, new DateSerializer())
    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
    .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeSerializer())
    .serializeNulls().create();

  static SerializedError serializeError(Throwable e) {
    SerializedError result = new SerializedError();
    result.error = new SerializedError.Error();
    result.error.message = e.getMessage();
    result.error.name = e.getClass().getName();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    e.printStackTrace(new PrintStream(out));
    result.error.stack = new String(out.toByteArray(), StandardCharsets.UTF_8);
    return result;
  }

  static SerializedArgument serializeArgument(Object arg) {
    return new ValueSerializer(arg).toSerializedArgument();
  }

  static <T> T deserialize(SerializedValue value) {
    return deserialize(value, new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  private static <T> T deserialize(SerializedValue value, Map<Integer, Object> idToValue) {
    if (value.ref != null) {
      return (T) idToValue.get(value.ref);
    }
    if (value.n != null) {
      if (value.n.doubleValue() == (double) value.n.intValue()) {
        return (T) Integer.valueOf(value.n.intValue());
      }
      return (T) Double.valueOf(value.n.doubleValue());
    }
    if (value.b != null)
      return (T) value.b;
    if (value.s != null)
      return (T) value.s;
    if (value.u != null) {
      try {
        return (T)(new URL(value.u));
      } catch (MalformedURLException e) {
        throw new PlaywrightException("Unexpected value: " + value.u, e);
      }
    }
    if (value.bi != null) {
      return (T) new BigInteger(value.bi);
    }
    if (value.d != null)
      return (T)(Date.from(Instant.parse(value.d)));
    if (value.r != null)
      return (T)(Pattern.compile(value.r.p, fromJsRegexFlags(value.r.f)));
    if (value.e != null) {
      return (T)new Exception(value.e.s);
    }
    if (value.v != null) {
      switch (value.v) {
        case "undefined":
        case "null":
          return null;
        case "Infinity":
          return (T) Double.valueOf(Double.POSITIVE_INFINITY);
        case "-Infinity":
          return (T) Double.valueOf(Double.NEGATIVE_INFINITY);
        case "-0": {
          return (T) Double.valueOf(-0.0);
        }
        case "NaN":
          return (T) Double.valueOf(Double.NaN);
        default:
          throw new PlaywrightException("Unexpected value: " + value.v);
      }
    }
    if (value.a != null) {
      List<Object> list = new ArrayList<>();
      idToValue.put(value.id, list);
      for (SerializedValue v : value.a) {
        list.add(deserialize(v, idToValue));
      }
      return (T) list;
    }
    if (value.o != null) {
      Map<String, Object> map = new LinkedHashMap<>();
      idToValue.put(value.id, map);
      for (SerializedValue.O o : value.o) {
        map.put(o.k, deserialize(o.v, idToValue));
      }
      return (T) map;
    }
    if (value.m != null) {
      Map<?, ?> map = new LinkedHashMap<>();
      idToValue.put(value.id, map);
      return (T) map;
    }
    if (value.se != null) {
      Map<?, ?> map = new LinkedHashMap<>();
      idToValue.put(value.id, map);
      return (T) map;
    }
    throw new PlaywrightException("Unexpected result: " + gson().toJson(value));
  }

  static JsonArray toJsonArray(Path[] files) {
    JsonArray jsonFiles = new JsonArray();
    for (Path p : files) {
      jsonFiles.add(p.toAbsolutePath().toString());
    }
    return jsonFiles;
  }

  static JsonArray toJsonArray(FilePayload[] files) {
    JsonArray jsonFiles = new JsonArray();
    for (FilePayload p : files) {
      jsonFiles.add(toProtocol(p));
    }
    return jsonFiles;
  }

  static JsonObject toProtocol(FilePayload p) {
    JsonObject jsonFile = new JsonObject();
    jsonFile.addProperty("name", p.name);
    jsonFile.addProperty("mimeType", p.mimeType);
    jsonFile.addProperty("buffer", Base64.getEncoder().encodeToString(p.buffer));
    return jsonFile;
  }

  static JsonArray toProtocol(ElementHandle[] handles) {
    JsonArray jsonElements = new JsonArray();
    for (ElementHandle handle : handles) {
      jsonElements.add(((ElementHandleImpl) handle).toProtocolRef());
    }
    return jsonElements;
  }

  public static JsonArray toProtocol(Map<String, String> map) {
    for (String value : map.values()) {
      if (value == null) {
        throw new PlaywrightException("Value cannot be null");
      }
    }
    return toNameValueArray(map.entrySet());
  }

  static void addHarUrlFilter(JsonObject options, Object urlFilter) {
      if (urlFilter instanceof String) {
        options.addProperty("urlGlob", (String) urlFilter);
      } else if (urlFilter instanceof Pattern) {
        Pattern pattern = (Pattern) urlFilter;
        options.addProperty("urlRegexSource", pattern.pattern());
        options.addProperty("urlRegexFlags", toJsRegexFlags(pattern));
      }
  }

  static JsonArray toNameValueArray(Iterable<? extends Map.Entry<String, ?>> collection) {
    JsonArray array = new JsonArray();
    for (Map.Entry<String, ?> e : collection) {
      JsonObject item = new JsonObject();
      item.addProperty("name", e.getKey());
      if (e.getValue() instanceof FilePayload) {
        item.add("value", gson().toJsonTree(e.getValue()));
      } else {
        item.addProperty("value", "" + e.getValue());
      }
      array.add(item);
    }
    return array;
  }

  static JsonArray toSelectValueOrLabel(String[] values) {
    JsonArray jsonOptions = new JsonArray();
    for (String value : values) {
      JsonObject option = new JsonObject();
      option.addProperty("valueOrLabel", value);
      jsonOptions.add(option);
    }
    return jsonOptions;
  }

  static Map<String, String> fromNameValues(JsonArray array) {
    Map<String, String> map = new LinkedHashMap<>();
    for (JsonElement element : array) {
      JsonObject pair = element.getAsJsonObject();
      map.put(pair.get("name").getAsString(), pair.get("value").getAsString());
    }
    return map;
  }

  static List<String> parseStringList(JsonArray array) {
    List<String> result = new ArrayList<>();
    for (JsonElement e : array) {
      result.add(e.getAsString());
    }
    return result;
  }

  private static DateFormat iso8601Format() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat;
  }
  public static final DateFormat dateFormat = iso8601Format();
}

