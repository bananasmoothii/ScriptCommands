/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0(the "License");
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
import fr.bananasmoothii.scriptcommands.core.execution.AbstractScriptException;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This class has as objective to replace all methods in {@link AbstractList} and all super interfaces by their overload
 * with a {@link Nullable @Nullable} {@link Context}. It also resolves some conflicts.<br/>
 * Warning: all exception thrown by this class should be {@link AbstractScriptException}s, meaning either
 * {@link ScriptException} or {@link ScriptException.Incomplete}. All other exceptions should be wrapped (using
 * {@link ScriptException.Incomplete#wrapInShouldNotHappen(Throwable)} or similar)<br/>
 * Warning 2: all implementations should be thread-safe.
 * @param <E> the type of each {@link ScriptValue}
 * @see AbstractScriptValueMap
 */
public abstract class AbstractScriptValueList<E> extends AbstractList<ScriptValue<E>> implements ScriptValueCollection,
        ScriptValueIterable<E>, ContextFixable {

    public abstract int size(@Nullable Context context);
    @Override @UseContext
    public int size() {
        return size(context);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEmpty(@Nullable Context context) {
        return size(context) == 0;
    }
    @Override @UseContext
    public boolean isEmpty() {
        return size() == 0;
    }

    public abstract boolean add(ScriptValue<E> element, @Nullable Context context);
    @Override @UseContext
    public boolean add(ScriptValue<E> element) {
        return add(element, context);
    }

    public void add(int index, ScriptValue<E> element, @Nullable Context context) {
        super.add(index, element);
    }
    @Override @UseContext
    public void add(int index, ScriptValue<E> element) {
        add(index, element, context);
    }

    @SuppressWarnings("unused")
    public boolean addAll(int index, Collection<? extends ScriptValue<E>> c, @Nullable Context context) {
        return super.addAll(index, c);
    }
    @Override @UseContext
    public boolean addAll(int index, Collection<? extends ScriptValue<E>> c) {
        return super.addAll(index, c);
    }

    @SuppressWarnings("unused")
    public boolean addAll(Collection<? extends ScriptValue<E>> c, @Nullable Context context) {
        return super.addAll(c);
    }
    @Override @UseContext
    public boolean addAll(Collection<? extends ScriptValue<E>> c) {
        return addAll(c, context);
    }

    public abstract ScriptValue<E> set(int index, ScriptValue<E> element, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<E> set(int index, ScriptValue<E> element) {
        return set(index, element, context);
    }

    public abstract ScriptValue<E> get(int index, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<E> get(int index) {
        return get(index, context);
    }

    public abstract ScriptValue<E> remove(int index, @Nullable Context context);
    @Override @UseContext
    public ScriptValue<E> remove(int index) {
        return remove(index, context);
    }

    public abstract boolean remove(Object o, @Nullable Context context);
    @Override @UseContext
    public boolean remove(Object o) {
        return remove(o, context);
    }

    public abstract int indexOf(Object o, @Nullable Context context);
    @Override @UseContext
    public int indexOf(Object o) {
        return indexOf(o, context);
    }

    public abstract int lastIndexOf(Object o, @Nullable Context context);
    @Override @UseContext
    public int lastIndexOf(Object o) {
        return lastIndexOf(o, context);
    }

    public abstract void clear(@Nullable Context context);
    @Override @UseContext
    public void clear() {
        clear(context);
    }

    public @NotNull Iterator<ScriptValue<E>> iterator(@Nullable Context context) {
        return new Itr(context);
    }

    public @NotNull ListIterator<ScriptValue<E>> listIterator(@Nullable Context context) {
        return new Itr(context);
    }
    @Override @UseContext
    public @NotNull ListIterator<ScriptValue<E>> listIterator() {
        return listIterator(context);
    }

    public @NotNull ListIterator<ScriptValue<E>> listIterator(int index, @Nullable Context context) {
        return new Itr(index, context);
    }
    @Override @UseContext
    public @NotNull ListIterator<ScriptValue<E>> listIterator(int index) {
        return listIterator(index, context);
    }

    /**
     * Just a little method that takes advantage of the custom implementation of iterators, even if it is likely not
     * being used
     */
    @SuppressWarnings("unused")
    public @NotNull ListIterator<ScriptValue<E>> listIterator(int fromIndex, int toIndex, @Nullable Context context) {
        return new Itr(fromIndex, toIndex, context);
    }

    /**
     * This is not thread-safe, but scripts shouldn't be able to use an iterator in a thread.
     */
    protected class Itr implements ListIterator<ScriptValue<E>> {

        protected int fromIndex;
        protected int toIndex;
        protected int currentIndex;
        protected final Context context;

        protected Itr(@Nullable Context context) {
            this(0, size(context), context);
        }

        /**  
         * @param fromIndex inclusive
         */
        protected Itr(int fromIndex, @Nullable Context context) {
            this(fromIndex, size(context), context);
        }

        /**
         * @param fromIndex inclusive
         * @param toIndex exclusive
         */
        protected Itr(int fromIndex, int toIndex, @Nullable Context context) {
            this.context = context;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            this.currentIndex = fromIndex;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < toIndex;
        }

        @Override
        public ScriptValue<E> next() {
            rangeCheck(currentIndex);
            return get(currentIndex++, context);
        }

        @Override
        public boolean hasPrevious() {
            return currentIndex >= fromIndex;
        }

        @Override
        public ScriptValue<E> previous() {
            rangeCheck(currentIndex);
            return get(currentIndex--, context);
        }

        @Override
        public int nextIndex() {
            return currentIndex + 1;
        }

        @Override
        public int previousIndex() {
            return currentIndex - 1;
        }

        @Override
        public void remove() {
            AbstractScriptValueList.this.remove(currentIndex, context);
            toIndex--;
        }

        @Override
        public void set(ScriptValue<E> element) {
            AbstractScriptValueList.this.set(currentIndex,element, context);
        }

        @Override
        public void add(ScriptValue<E> element) {
            AbstractScriptValueList.this.add(currentIndex, element, context);
            toIndex++;
        }

        protected void rangeCheck(int index) {
            if (index < fromIndex || index >= toIndex) throw new ScriptException.Incomplete(ScriptException.ExceptionType.OUT_OF_BOUNDS,
                    "Invalid index: " + index + " for list iterator from " + fromIndex + " to " + toIndex).completeIfPossible(context);
        }
    }

    @Override
    public Spliterator<ScriptValue<E>> spliterator(@Nullable Context context) {
        return ScriptValueIterable.super.spliterator(context);
    }

    @Override
    public Spliterator<ScriptValue<E>> spliterator() {
        return ScriptValueIterable.super.spliterator();
    }

    /**
     * This is a bit different than {@link AbstractList#subList(int, int) the default subList} because here,
     * it is a totally independent list, and modifying one list won't affect the other, except if you modify elements
     * in it.
     * @param fromIndex inclusive
     * @param toIndex exclusive
     */
    public AbstractScriptValueList<E> subList(int fromIndex, int toIndex, @Nullable Context context) {
        rangeCheck(fromIndex, context);
        rangeCheck(toIndex,  context);
        AbstractScriptValueList<E> newList = new ScriptValueList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            newList.add(get(i, context), context); // context context context context yum good context everywhere (I'm tired)
        }
        return newList;
    }
    /**
     * @see #subList(int, int, Context)
     */
    @Override
    public AbstractScriptValueList<E> subList(int fromIndex, int toIndex) {
        return subList(fromIndex, toIndex, context);
    }

    public abstract boolean contains(Object o, @Nullable Context context);
    @Override @UseContext
    public boolean contains(Object o) {
        return contains(o, context);
    }

    public abstract AbstractScriptValueList<E> clone();

    @Override
    public abstract boolean makeSQL(@Nullable Context context);

    @Override
    public Object toNormalClasses() {
        return ScriptValueCollection.super.toNormalClasses();
    }

    @Override
    public abstract Object toNormalClasses(boolean forJson);

    @Override
    public abstract @Nullable String getSQLTable();

    @Override
    public abstract boolean canUseSQL();

    @Override
    public abstract @NotNull StringID getStringID();

    @Override
    public abstract char getTypeChar();

    /**
     * Checks if the given index is in range. If not, throws a {@link AbstractScriptException}
     * @see ArrayList#remove(int)
     */
    protected void rangeCheck(int index, @Nullable Context context) {
        if (index < 0 || index >= size(context))
            throw new ScriptException.Incomplete(ScriptException.ExceptionType.OUT_OF_BOUNDS, outOfBoundsMsg(index, context))
                    .completeIfPossible(context);
    }

    protected void rangeCheckForAdd(int index, @Nullable Context context) {
        if (index < 0 || index > size(context))
            throw new ScriptException.Incomplete(ScriptException.ExceptionType.OUT_OF_BOUNDS, outOfBoundsMsg(index, context))
                    .completeIfPossible(context);

    }

    protected String outOfBoundsMsg(int index, @Nullable Context context) {
        return "Index: "+index+", Size: "+size(context);
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
}
