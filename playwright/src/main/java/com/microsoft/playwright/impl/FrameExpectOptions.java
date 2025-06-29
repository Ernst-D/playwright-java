package com.microsoft.playwright.impl;

import java.util.List;

class FrameExpectOptions {
  Object expressionArg;
  List<ExpectedTextValue> expectedText;
  Double expectedNumber;
  SerializedArgument expectedValue;
  Boolean useInnerText;
  boolean isNot;
  Double timeout;
}
