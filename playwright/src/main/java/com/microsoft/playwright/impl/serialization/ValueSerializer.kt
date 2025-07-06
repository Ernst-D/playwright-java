package com.microsoft.playwright.impl.serialization;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.impl.Channel;
import com.microsoft.playwright.impl.JSHandleImpl;
import com.microsoft.playwright.impl.SerializedArgument;
import com.microsoft.playwright.impl.SerializedValue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

import static com.microsoft.playwright.impl.Utils.toJsRegexFlags;

// part of serialization.java, move to single object later
public class ValueSerializer {
  // hashCode() of a map containing itself as a key will throw stackoverflow exception,
  // so we user wrappers.
  private static class HashableValue {
    final Object value;
    HashableValue(Object value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      return value == ((HashableValue) o).value;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(value);
    }
  }
  private final Map<HashableValue, Integer> valueToId = new HashMap<>();
  private int lastId = 0;
  private final List<JSHandleImpl> handles = new ArrayList<>();
  private final SerializedValue serializedValue;

  public ValueSerializer(Object value) {
    serializedValue = serializeValue(value);
  }

  public SerializedArgument toSerializedArgument() {
    SerializedArgument result = new SerializedArgument();
    result.value = serializedValue;
    result.handles = new Channel[handles.size()];
    int i = 0;
    for (JSHandleImpl handle : handles) {
      result.handles[i] = new Channel();
      result.handles[i].guid = handle.guid;
      ++i;
    }
    return result;
  }

  private SerializedValue serializeValue(Object value) {
    SerializedValue result = new SerializedValue();
    if (value instanceof JSHandleImpl) {
      result.h = handles.size();
      handles.add((JSHandleImpl) value);
      return result;
    }
    if (value == null) {
      result.v = "undefined";
    } else if (value instanceof Double) {
      double d = ((Double) value);
      if (d == Double.POSITIVE_INFINITY) {
        result.v = "Infinity";
      } else if (d == Double.NEGATIVE_INFINITY) {
        result.v = "-Infinity";
      } else if (d == -0) {
        result.v = "-0";
      } else if (Double.isNaN(d)) {
        result.v = "NaN";
      } else {
        result.n = d;
      }
    } else if (value instanceof Boolean) {
      result.b = (Boolean) value;
    } else if (value instanceof Integer) {
      result.n = (Integer) value;
    } else if (value instanceof String) {
      result.s = (String) value;
    } else if (value instanceof Date) {
      result.d = ((Date)value).toInstant().toString();
    } else if (value instanceof LocalDateTime) {
      result.d = ((LocalDateTime)value).atZone(ZoneId.systemDefault()).toInstant().toString();
    } else if (value instanceof URL) {
      result.u = ((URL)value).toString();
    } else if (value instanceof BigInteger) {
      result.bi = ((BigInteger)value).toString();
    } else if (value instanceof Pattern) {
      result.r = new SerializedValue.R();
      result.r.p = ((Pattern)value).pattern();
      result.r.f = toJsRegexFlags(((Pattern)value));
    } else if (value instanceof Exception) {
      Exception exception = (Exception) value;
      result.e = new SerializedValue.E();
      result.e.m = exception.getMessage();
      result.e.n = exception.getClass().getSimpleName();
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      result.e.s = sw.toString();
    } else {
      HashableValue mapKey = new HashableValue(value);
      Integer id = valueToId.get(mapKey);
      if (id != null) {
        result.ref = id;
      } else {
        result.id = ++lastId;
        valueToId.put(mapKey, lastId);
        if (value instanceof List) {
          List<SerializedValue> list = new ArrayList<>();
          for (Object o : (List<?>) value) {
            list.add(serializeValue(o));
          }
          result.a = list.toArray(new SerializedValue[0]);
        } else if (value instanceof Map) {
          List<SerializedValue.O> list = new ArrayList<>();
          @SuppressWarnings("unchecked")
          Map<String, ?> map = (Map<String, ?>) value;
          for (Map.Entry<String, ?> e : map.entrySet()) {
            SerializedValue.O o = new SerializedValue.O();
            o.k = e.getKey();
            o.v = serializeValue(e.getValue());
            list.add(o);
          }
          result.o = list.toArray(new SerializedValue.O[0]);
        } else if (value instanceof Object[]) {
          List<SerializedValue> list = new ArrayList<>();
          for (Object o : (Object[]) value) {
            list.add(serializeValue(o));
          }
          result.a = list.toArray(new SerializedValue[0]);
        } else {
          throw new PlaywrightException("Unsupported type of argument: " + value);
        }
      }
    }
    return result;
  }
}
