package com.microsoft.playwright.impl

import com.microsoft.playwright.options.HarContentPolicy
import java.nio.file.Path

/**
 * part of BrowserContextImpl
 */
class HarRecorder (@JvmField val path: Path?, @JvmField val contentPolicy: HarContentPolicy?)
