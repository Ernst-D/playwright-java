package com.microsoft.playwright.impl

// part of the protocol.java
internal class SerializedValue
{
    @JvmField
    var n: Number? = null
    @JvmField
    var b: Boolean? = null
    @JvmField
    var s: String? = null

    // Possible values: { 'null, 'undefined, 'NaN, 'Infinity, '-Infinity, '-0 }
    @JvmField
    var v: String? = null
    @JvmField
    var d: String? = null
    @JvmField
    var u: String? = null
    @JvmField
    var bi: String? = null

    class E
    {
        @JvmField
        var m: String? = null
        @JvmField
        var n: String? = null
        @JvmField
        var s: String? = null
    }

    @JvmField
    var e: E? = null

    class R
    {
        @JvmField
        var p: String? = null
        @JvmField
        var f: String? = null
    }

    @JvmField
    var r: R? = null
    @JvmField
    var a: Array<SerializedValue?>? = null

    class O
    {
        @JvmField
        var k: String? = null
        @JvmField
        var v: SerializedValue? = null
    }

    @JvmField
    var o: Array<O?>? = null
    @JvmField
    var h: Number? = null
    @JvmField
    var id: Int? = null
    @JvmField
    var ref: Int? = null

    // JS representation of Map: [[key1, value1], [key2, value2], ...].
    @JvmField
    var m: SerializedValue? = null

    // JS representation of Set: [item1, item2, ...].
    @JvmField
    var se: SerializedValue? = null
}
