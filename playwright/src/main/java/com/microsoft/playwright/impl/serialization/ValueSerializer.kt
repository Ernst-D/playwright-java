package com.microsoft.playwright.impl.serialization

import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.impl.*
import com.microsoft.playwright.impl.SerializedValue.*
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Double
import java.math.BigInteger
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.regex.Pattern
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.arrayOfNulls

// part of serialization.java, move to single object later
class ValueSerializer(value: Any?)
{
    // hashCode() of a map containing itself as a key will throw stackoverflow exception,
    // so we user wrappers.
    private class HashableValue(val value: Any?)
    {
        override fun equals(other: Any?): Boolean
        {
            return value === (other as HashableValue).value
        }

        override fun hashCode(): Int
        {
            return System.identityHashCode(value)
        }
    }

    private val valueToId: MutableMap<HashableValue?, Int?> = HashMap<HashableValue?, Int?>()
    private var lastId = 0
    private val handles: MutableList<JSHandleImpl> = ArrayList<JSHandleImpl>()
    private val serializedValue: SerializedValue

    init
    {
        serializedValue = serializeValue(value)
    }

    fun toSerializedArgument(): SerializedArgument
    {
        val result = SerializedArgument()
        result.value = serializedValue
        result.handles = arrayOfNulls<Channel>(handles.size)
        var i = 0
        for (handle in handles)
        {
            result.handles!![i] = Channel()
            result.handles!![i]!!.guid = handle.guid
            ++i
        }
        return result
    }

    private fun serializeValue(value: Any?): SerializedValue
    {
        val result = SerializedValue()
        if (value is JSHandleImpl)
        {
            result.h = handles.size
            handles.add(value)
            return result
        }
        if (value == null)
        {
            result.v = "undefined"
        } else if (value is kotlin.Double)
        {
            val d = value
            if (d == Double.POSITIVE_INFINITY)
            {
                result.v = "Infinity"
            } else if (d == Double.NEGATIVE_INFINITY)
            {
                result.v = "-Infinity"
            } else if (d == -0.0)
            {
                result.v = "-0"
            } else if (Double.isNaN(d))
            {
                result.v = "NaN"
            } else
            {
                result.n = d
            }
        } else if (value is Boolean)
        {
            result.b = value
        } else if (value is Int)
        {
            result.n = value
        } else if (value is String)
        {
            result.s = value
        } else if (value is Date)
        {
            result.d = value.toInstant().toString()
        } else if (value is LocalDateTime)
        {
            result.d = value.atZone(ZoneId.systemDefault()).toInstant().toString()
        } else if (value is URL)
        {
            result.u = value.toString()
        } else if (value is BigInteger)
        {
            result.bi = value.toString()
        } else if (value is Pattern)
        {
            result.r = R()
            result.r!!.p = value.pattern()
            result.r!!.f = Utils.toJsRegexFlags(value)
        } else if (value is Exception)
        {
            val exception = value
            result.e = E()
            result.e!!.m = exception.message
            result.e!!.n = exception.javaClass.getSimpleName()
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            result.e!!.s = sw.toString()
        } else
        {
            val mapKey = HashableValue(value)
            val id = valueToId.get(mapKey)
            if (id != null)
            {
                result.ref = id
            } else
            {
                result.id = ++lastId
                valueToId.put(mapKey, lastId)
                if (value is MutableList<*>)
                {
                    val list: MutableList<SerializedValue?> = ArrayList<SerializedValue?>()
                    for (o in value)
                    {
                        list.add(serializeValue(o))
                    }
                    result.a = list.toTypedArray<SerializedValue?>()
                } else if (value is MutableMap<*, *>)
                {
                    val list: MutableList<O?> = ArrayList<O?>()
                    val map = value as MutableMap<String?, *>
                    for (e in map.entries)
                    {
                        val o = O()
                        o.k = e.key
                        o.v = serializeValue(e.value)
                        list.add(o)
                    }
                    result.o = list.toTypedArray<O?>()
                } else if (value is Array<*> && value.isArrayOf<Any>())
                {
                    val list: MutableList<SerializedValue?> = ArrayList<SerializedValue?>()
                    for (o in value as Array<Any?>)
                    {
                        list.add(serializeValue(o))
                    }
                    result.a = list.toTypedArray<SerializedValue?>()
                } else
                {
                    throw PlaywrightException("Unsupported type of argument: " + value)
                }
            }
        }
        return result
    }
}
