package com.microsoft.playwright.impl;

import com.microsoft.playwright.options.HarContentPolicy;

import java.nio.file.Path;

public class HarRecorder {
  final Path path;
  final HarContentPolicy contentPolicy;

  HarRecorder(Path har, HarContentPolicy policy) {
    path = har;
    contentPolicy = policy;
  }
}
