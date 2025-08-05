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

import java.util.function.Consumer
import java.util.function.Predicate

internal open class WaitableEvent<EventType, T> @JvmOverloads constructor(
    val listeners: ListenerCollection<EventType?>,
    private val type: EventType?,
    private val predicate: Predicate<T?>? = null
) : Waitable<T?>, Consumer<T?>
{
    private var eventArg: T? = null

    init
    {
        listeners.add(type, this)
    }

    override fun accept(eventArg: T?)
    {
        if (predicate != null && !predicate.test(eventArg))
        {
            return
        }

        this.eventArg = eventArg
        dispose()
    }

    override fun isDone(): Boolean
    {
        return eventArg != null
    }

    override fun dispose()
    {
        listeners.remove(type, this)
    }

    override fun get(): T?
    {
        return eventArg
    }
}
