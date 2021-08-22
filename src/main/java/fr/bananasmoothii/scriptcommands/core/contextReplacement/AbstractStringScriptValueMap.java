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

import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueCollection;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringID;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Basically copy/pasted from {@link AbstractScriptValueMap} for {@link fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap}
 * because there are some incompatibilities between {@link String} and {@link ScriptValue}
 */
public abstract class AbstractStringScriptValueMap<V> extends AbstractMap<String, ScriptValue<V>> implements ScriptValueCollection,
        ScriptValueIterable<ScriptValueList<Object>>, ContextFixable {

    public abstract int size(@Nullable Context context);
    @Override @UseContext
    public int size() {
        return size(context);
    }

    public boolean isEmpty(@Nullable Context context) {
        return size(context) == 0;
    }
    @Override @UseContext
    public boolean isEmpty() {
        return isEmpty(context);
    }

    public abstract boolean containsValue(Object value, @Nullable Context context);
    @Override @UseContext
    public boolean containsValue(Object value) {
        return containsValue(value, context);
    }

    public abstract boolean containsKey(Object key, @Nullable Context context);
    @Override @UseContext
    public boolean containsKey(Object key) {
        return containsKey(key, context);
    }

    public abstract ScriptValue<V> get(Object key, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<V> get(Object key) {
        return get(key, context);
    }

    public abstract ScriptValue<V> put(String key, ScriptValue<V> value, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<V> put(String key, ScriptValue<V> value) {
        return put(key, value, context);
    }

    @SuppressWarnings("unused")
    public @Nullable ScriptValue<V> putIfAbsent(String key, ScriptValue<V> value, @Nullable Context context) {
        return super.putIfAbsent(key, value);
    }
    @Override @UseContext
    public @Nullable ScriptValue<V> putIfAbsent(String key, ScriptValue<V> value) {
        return super.putIfAbsent(key, value);
    }

    public abstract ScriptValue<V> remove(Object key, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<V> remove(Object key) {
        return remove(key, context);
    }

    public abstract void clear(@Nullable Context context);
    @Override @UseContext
    public void clear() {
        clear(context);
    }

    public abstract @NotNull Set<String> keySet(@Nullable Context context);
    @Override @UseContext
    public @NotNull Set<String> keySet() {
        return keySet(context);
    }

    public abstract @NotNull ScriptValueList<V> values(@Nullable Context context);
    @Override @UseContext
    public @NotNull ScriptValueList<V> values() {
        return values(context);
    }

    public abstract @NotNull Set<Entry<String, ScriptValue<V>>> entrySet(@Nullable Context context);
    @Override @UseContext
    public @NotNull Set<Entry<String, ScriptValue<V>>> entrySet() {
        return entrySet(context);
    }

    @Override
    public @NotNull Iterator<ScriptValue<ScriptValueList<Object>>> iterator(@Nullable Context context) {
        Iterator<Entry<String, ScriptValue<V>>> entryIterator = entrySet(context).iterator();
        return new Iterator<ScriptValue<ScriptValueList<Object>>>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public ScriptValue<ScriptValueList<Object>> next() {
                ScriptValueList<Object> pair = new ScriptValueList<>();
                Entry<String, ScriptValue<V>> next = entryIterator.next();
                pair.add(new ScriptValue<>(next.getKey()));
                //noinspection unchecked
                pair.add((ScriptValue<Object>) next.getValue());
                return new ScriptValue<>(pair);
            }
        };
    }

    protected @Nullable Context context;

    @Override
    public void setFixedContext(@Nullable Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Context getFixedContext() {
        return context;
    }

    @Override
    public abstract Object toNormalClasses(boolean forJson);

    @Override
    public abstract @Nullable String getSQLTable();

    @Override
    public abstract char getTypeChar();

    @Override
    public abstract @NotNull StringID getStringID();

    @Override
    public abstract boolean canUseSQL();

    @Override
    public abstract boolean makeSQL(@Nullable Context context);

    @Override
    public abstract ScriptValueCollection clone();
}
