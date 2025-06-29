package com.microsoft.playwright.impl;


public class SerializedError{
  public static class Error {
    String message;
    String name;
    String stack;

    @Override
    public String toString() {
      return "Error {\n" +
        "  message='" + message + '\n' +
        "  name='" + name + '\n' +
        "  stack='" + stack + '\n' +
        '}';
    }
  }
  Error error;
  SerializedValue value;

  @Override
  public String toString() {
    if (error != null) {
      return error.toString();
    }
    return "SerializedError{" +
      "value=" + value +
      '}';
  }
}
