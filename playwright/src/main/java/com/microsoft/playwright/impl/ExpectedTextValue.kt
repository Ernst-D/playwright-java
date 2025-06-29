package com.microsoft.playwright.impl

// part of the protocol.java
internal class ExpectedTextValue
{
    @JvmField
    var string: String? = null
    @JvmField
    var regexSource: String? = null
    @JvmField
    var regexFlags: String? = null
    @JvmField
    var ignoreCase: Boolean? = null
    @JvmField
    var matchSubstring: Boolean? = null
    @JvmField
    var normalizeWhiteSpace: Boolean? = null
}
