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
package com.microsoft.playwright.impl

import org.opentest4j.AssertionFailedError
import org.opentest4j.ValueWrapper
import java.lang.String
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.text.isEmpty
import kotlin.toString

internal open class AssertionsBase(@JvmField val actualLocator: LocatorImpl, @JvmField val isNot: Boolean)
{
    fun expectImpl(
        expression: kotlin.String, textValue: ExpectedTextValue, expected: Any?, message: kotlin.String, options: FrameExpectOptions?
    )
    {
        expectImpl(expression, mutableListOf(textValue), expected, message, options)
    }

    fun expectImpl(
        expression: kotlin.String,
        expectedText: MutableList<ExpectedTextValue?>?,
        expected: Any?,
        message: kotlin.String,
        options: FrameExpectOptions?
    )
    {
        var options = options
        if (options == null)
        {
            options = FrameExpectOptions()
        }
        options.expectedText = expectedText
        expectImpl(expression, options, expected, message)
    }

    fun expectImpl(expression: kotlin.String, expectOptions: FrameExpectOptions, expected: Any?, message: kotlin.String)
    {
        var message: kotlin.String = message
        if (expectOptions.timeout == null)
        {
            expectOptions.timeout = AssertionsTimeout.defaultTimeout
        }
        expectOptions.isNot = isNot
        if (isNot)
        {
            message = message.replace("expected to", "expected not to")
        }
        val result = actualLocator.expect(expression, expectOptions)
        if (result.matches == isNot)
        {
            val actual = if (result.received == null) null else Serialization.deserialize<Any?>(result.received)
            var log = if (result.log == null) "" else String.join("\n", result.log)
            if (!log.isEmpty())
            {
                log = "\nCall log:\n$log"
            }
            if (expected == null)
            {
                throw AssertionFailedError(message + log)
            }
            val expectedValue: ValueWrapper = formatValue(expected)
            val actualValue: ValueWrapper = formatValue(actual)
            message += ": " + expectedValue.stringRepresentation + "\nReceived: " + actualValue.getStringRepresentation() + "\n"
            throw AssertionFailedError(message + log, expectedValue, actualValue)
        }
    }

    companion object
    {
        private fun formatValue(value: Any?): ValueWrapper
        {
            if (value == null || !value.javaClass.isArray())
            {
                return ValueWrapper.create(value)
            }
            val values: MutableCollection<kotlin.String?> =
                listOf(*value as Array<Any?>).stream().map { e: Any? -> e.toString() }
                    .collect(Collectors.toList())
            val stringRepresentation = "[" + String.join(", ", values) + "]"
            return ValueWrapper.create(value, stringRepresentation)
        }

        @JvmStatic
        fun expectedRegex(pattern: Pattern): ExpectedTextValue
        {
            val expected = ExpectedTextValue()
            expected.regexSource = pattern.pattern()
            if (pattern.flags() != 0)
            {
                expected.regexFlags = Utils.toJsRegexFlags(pattern)
            }
            return expected
        }

        @JvmStatic
        fun shouldIgnoreCase(options: Any?): Boolean?
        {
            if (options == null)
            {
                return null
            }
            try
            {
                val fromField = options.javaClass.getDeclaredField("ignoreCase")
                val value = fromField.get(options)
                return value as Boolean?
            } catch (_: NoSuchFieldException)
            {
                return null
            } catch (_: IllegalAccessException)
            {
                return null
            }
        }
    }
}
