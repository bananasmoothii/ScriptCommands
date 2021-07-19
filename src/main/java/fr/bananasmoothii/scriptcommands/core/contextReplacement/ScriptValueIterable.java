/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.core.contextReplacement;

import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface ScriptValueIterable<T> extends Iterable<ScriptValue<T>> {
    @NotNull
    @Override
    @UseContext
    default Iterator<ScriptValue<T>> iterator() {
        return iterator(contextForNextIterator.getAndSet(null));
    }

    @Override
    @UseContext
    default void forEach(Consumer<? super ScriptValue<T>> action) {
        forEach(action, contextForNextIterator.getAndSet(null));
    }

    @Override
    @UseContext
    default Spliterator<ScriptValue<T>> spliterator() {
        return spliterator(contextForNextIterator.getAndSet(null));
    }

    @NotNull
    Iterator<ScriptValue<T>> iterator(@Nullable Context context);

    default void forEach(Consumer<? super ScriptValue<T>> action, @Nullable Context context) {
        Iterable.super.forEach(action);
    }

    default Spliterator<ScriptValue<T>> spliterator(@Nullable Context context) {
        return Iterable.super.spliterator();
    }

    AtomicReference<@Nullable Context> contextForNextIterator = new AtomicReference<>();

    /**
     * This will make the next call to {@link #iterator()}, {@link #forEach(Consumer)} or {@link #spliterator()} use that
     * given {@link Context}. After one of the three gets called, you have to re-set the context if you want to reuse it.
     */
    default void setContextForNextIterator(@Nullable Context context) {
        contextForNextIterator.set(context);
    }
}
