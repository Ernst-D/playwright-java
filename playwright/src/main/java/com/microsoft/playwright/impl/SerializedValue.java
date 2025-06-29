package com.microsoft.playwright.impl;

class SerializedValue {
  Number n;
  Boolean b;
  String s;
  // Possible values: { 'null, 'undefined, 'NaN, 'Infinity, '-Infinity, '-0 }
  String v;
  String d;
  String u;
  String bi;

  public static class E {
    String m;
    String n;
    String s;
  }

  E e;

  public static class R {
    String p;
    String f;
  }

  R r;
  SerializedValue[] a;

  public static class O {
    String k;
    SerializedValue v;
  }

  O[] o;
  Number h;
  Integer id;
  Integer ref;
  // JS representation of Map: [[key1, value1], [key2, value2], ...].
  SerializedValue m;
  // JS representation of Set: [item1, item2, ...].
  SerializedValue se;
}
