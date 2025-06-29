package com.microsoft.playwright.impl

// part of the protocol.java
class SerializedError
{
    class Error
    {
        @JvmField
        var message: String? = null
        @JvmField
        var name: String? = null
        @JvmField
        var stack: String? = null

        override fun toString(): String
        {
            return "Error {\n" + "  message='" + message + '\n' + "  name='" + name + '\n' + "  stack='" + stack + '\n' + '}'
        }
    }

    @JvmField
    var error: Error? = null
    private var value: SerializedValue? = null

    override fun toString(): String
    {
        if (error != null)
        {
            return error.toString()
        }
        return "SerializedError{" + "value=" + value + '}'
    }
}
