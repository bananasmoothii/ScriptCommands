/*
 *    Copyright 2020 ScriptCommands
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueCollection;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.Incomplete;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ScriptStackTraceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

// TODO (but not for now): function type, that will save a Method (and not a runnable because we can't really save a runnable)

/**
 * Value for the Scripts. Each variable in the Scripts are in fact a ScriptValue.
 * @see ScriptValue#ScriptValue(Object) the constructor
 */
@SuppressWarnings("unchecked")
public class ScriptValue<T> implements Cloneable, Iterable<ScriptValue<?>> {
	
	/**
	* The value this class is made for
	*/
	public final @NotNull T v;

	public final ScriptValueType type;

	public static final ScriptValue<NoneType> NONE = //<editor-fold desc="faster implementation" defaultstate="collapsed">
	new ScriptValue<NoneType>(null) {
		@Override
		public @Nullable Object toNormalClass(boolean forJson) {
			return null;
		}

		@Override
		public int hashCode() {
			return 1; // as in NoneType
		}

		@Override
		public boolean is(ScriptValueType type) {
			return type == ScriptValueType.NONE;
		}

		@Override
		public boolean isNumber() {
			return false;
		}

		@Override
		public @NotNull Iterator<ScriptValue<?>> iterator() {
			throw Incomplete.invalidType("List, Text or Dictionary", "none");
		}
	};
	//</editor-fold>

	/**
	 * Value for the Scripts. Each variable in the Scripts are in fact a ScriptValue.
	 * @param obj of type T. Possible types are:
	 *     <ul>
	 *         <li>{@code null}, it will be converted to {@link NoneType} or</li>
	 *         <li>{@link NoneType} but please use {@link #NONE} instead</li>
	 *         <li>{@link Integer}</li>
	 *         <li>{@link Double}</li>
	 *         <li>{@link String}</li>
	 *         <li>{@link Boolean}</li>
	 *         <li>{@link ScriptValueList}{@literal <ScriptValue>}</li>
	 *         <li>{@link ScriptValueMap}{@literal <ScriptValue, ScriptValue>}</li>
	 *         <li>{@link List}{@literal <Object>} will be automatically converted to a {@link ScriptValueList}</li>
	 *         <li>{@link Map}{@literal <Object, Object>} will be automatically converted to a {@link ScriptValueMap}</li>
	 *     </ul>
	 * @see #NONE a better solution for getting a None ScriptValue
	 */
	public ScriptValue(@Nullable T obj) {

		v = obj == null ? (T) NoneType.INSTANCE : obj;
		if (obj == null || obj instanceof NoneType) type = ScriptValueType.NONE;
		else if (obj instanceof Integer)            type = ScriptValueType.INTEGER;
		else if (obj instanceof Double)             type = ScriptValueType.DECIMAL;
		else if (obj instanceof String)             type = ScriptValueType.TEXT;
		else if (obj instanceof Boolean)            type = ScriptValueType.BOOLEAN;
		else if (obj instanceof ScriptValueList)    type = ScriptValueType.LIST;
		else if (obj instanceof ScriptValueMap)     type = ScriptValueType.DICTIONARY;
		else throw new IllegalArgumentException(obj.getClass().getName()+
					" is not a valid argument, must be String, Integer, Double, " +
					"Boolean, ScriptValueList or ScriptValueMap .");
	}

	public enum ScriptValueType {
		NONE("None", (byte) 0),
		INTEGER("Integer", (byte) 1),
		DECIMAL("Decimal", (byte) 2),
		TEXT("Text", (byte) 3),
		BOOLEAN("Boolean", (byte) 4),
		LIST("List", (byte) 5),
		DICTIONARY("Dictionary", (byte) 6);


		/**
		 * Can be:
		 * <ul>
		 *     <li>None</li>
		 *     <li>Integer</li>
		 *     <li>Decimal</li>
		 *     <li>Text</li>
		 *     <li>Boolean</li>
		 *     <li>List</li>
		 *     <li>Dictionary</li>
		 * </ul>
		 */
		public String name;

		/**
		 * Will be:
		 * <ul>
		 *     <li>0 for None</li>
		 *     <li>1 for Integer</li>
		 *     <li>2 for Decimal</li>
		 *     <li>3 for Text</li>
		 *     <li>4 for Boolean</li>
		 *     <li>5 for List</li>
		 *     <li>6 for Dictionary</li>
		 * </ul>
		 */
		public byte asByte;

		ScriptValueType(String name, byte asByte) {
			this.name = name;
			this.asByte = asByte;
		}
	}

	/**
	 * Contrary of {@link #toNormalClass(boolean)}
	 * Basically the same as {@link #ScriptValue(Object)} but has an extra check for {@link Map}s and {@link List}s,
	 * and recursively converts inside them to ScriptValues.
	 * @param keysAreJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
	 * @return a new {@link ScriptValue}
	 */
	@SuppressWarnings("rawtypes")
	public static <T> ScriptValue<? extends T> toScriptValue(@Nullable T obj, boolean keysAreJson) {
		if (obj instanceof List) return new ScriptValue(ScriptValueList.toScriptValues((List<Object>) obj, keysAreJson));
		if (obj instanceof Map) return new ScriptValue(ScriptValueMap.toScriptValues((Map<Object, Object>) obj, keysAreJson));
		else return new ScriptValue<>(obj);
	}

	/**
	 * Contrary of {@link #toScriptValue(Object, boolean)}
	 * @return {@link Object} because for example, the normal class of
	 * {@link ScriptValueMap} is {@link HashMap}
	 */
	public @Nullable Object toNormalClass(boolean forJson) {
		if (is(ScriptValueType.NONE)) return null;
		else if (v instanceof ScriptValueCollection) return ((ScriptValueCollection) v).toNormalClasses(forJson);
		else return v;
	}

	private static final Pattern integerPattern = Pattern.compile("\\d+");
	private static final Pattern decimalPattern = Pattern.compile("\\d+\\.\\d+");

	/**
	 * This method is used for converting simple {@link ScriptValue}s that you got as {@link String} to a real ScriptValue,
	 * for example with "123.4" you will get a new ScriptValue as if you did {@code new ScriptValue(123.4)}.
	 * @throws IllegalArgumentException if the passed String cannot be converted to {@code Integer}, {@code Decimal} or
	 * {@code Boolean}.
	 */
	public static ScriptValue<?> notCollectionToScriptValue(String s) {
		if (s.equals("true")) return new ScriptValue<>(true);
		if (s.equals("false")) return new ScriptValue<>(false);
		// using regexes here because I don't want that to work if there are spaces or strange values
		if (integerPattern.matcher(s).matches()) return new ScriptValue<>(Integer.valueOf(s));
		if (decimalPattern.matcher(s).matches()) return new ScriptValue<>(Double.valueOf(s));
		throw new IllegalArgumentException("the passed String (\"" + s + "\") cannot be converted to Integer, Decimal or Boolean. " +
				"It seems like you modified the storage file, if you want to do so, please read the JavaDoc: ScriptValueMap#toNormalClasses(boolean)");
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof ScriptValue))
			return false;
		return v.equals(((ScriptValue<?>)o).v);
	}

	@Override
	public int hashCode() {
		return v.hashCode();
	}

	public boolean is(ScriptValueType type) {
		return this.type == type;
	}
	
	public boolean isNumber() {
		return is(ScriptValueType.INTEGER) || is(ScriptValueType.DECIMAL);
	}

	@NotNull
	@Override
	public Iterator<ScriptValue<?>> iterator() {
		return iterator(null, null);
	}

	public @NotNull Iterator<ScriptValue<?>> iterator(@NotNull ScriptException.ContextStackTraceElement where) {
		return iterator(where.context, where);
	}

	/**
	 * @param where to know where we are exactly in the script
	 * @return an iterator iterating all characters of a text, all elements of a list or all pairs ofa dictionary. (e.g.
	 * 		   if your dictionary is [1=2, 3=4], and you want to iterate over it (with a for loop), you will get first
	 * 		   [1, 2], then [3, 4]
	 */
	public Iterator<ScriptValue<?>> iterator(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (is(ScriptValueType.LIST)) {
			return ((ScriptValueList) v).iterator(); // idk what to do about this raw type
		}
		if (is(ScriptValueType.TEXT)) {
			return new Iterator<ScriptValue<?>>() {
				private int currentIndex = 0;
				private final String string = (String) v;
				private final int size = string.length();

				@Override
				public boolean hasNext() {
					return currentIndex < size;
				}

				@Override
				public ScriptValue<?> next() {
					return new ScriptValue<>(String.valueOf(string.charAt(currentIndex++)));
				}
			};
		}
		if (is(ScriptValueType.DICTIONARY)) {
			return new Iterator<ScriptValue<?>>() {
				private final Iterator<Entry<ScriptValue<Object>, ScriptValue<Object>>> iterator = ((ScriptValueMap<Object, Object>) v).entrySet(context).iterator();

				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public ScriptValue<?> next() {
					ScriptValueList<Object> pair = new ScriptValueList<>();
					Entry<ScriptValue<Object>, ScriptValue<Object>> next = iterator.next();
					pair.add(next.getKey(), context);
					pair.add(next.getValue(), context);
					return new ScriptValue<>(pair);
				}
			};
		}
		throw Incomplete.invalidType("List, Text or Dictionary", this, where).completeIfPossible(context);
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public ScriptValue<T> clone() {
		switch (type) {
			case LIST:
			case DICTIONARY:
				return new ScriptValue<>((T) ((ScriptValueCollection) v).clone());
			default:
				return this;
		}
	}

	
	public Class<?> getValueClass() {
		return v.getClass();
	}

	@Override
	public String toString() {
		if (is(ScriptValueType.TEXT))
			return (String) v;
		return Types.getPrettyArg(this);
	}

	/**
	 * Not the same as {@link ScriptValue#toString()} because this can throw an error, if the value is not a string.
	 */
	public String asString() {
		return asString(null, null);
	}

	/**
	 * Not the same as {@link ScriptValue#toString()} because this can throw an error, if the value is not a string.
	 */
	public String asString(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof String) return (String) v;
		throw Incomplete.invalidType("Text", this, where).completeIfPossible(context);
	}

	/**
	 * This can auto-cast an integer to a double
	 */
	public double asDouble() {
		return asDouble(null, null);
	}

	/**
	 * This can auto-cast an integer to a double
	 */
	public double asDouble(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof Double) return (Double) v;
		if (v instanceof Integer) return (int) (Integer) v;
		throw Incomplete.invalidType("Decimal or Integer", this, where).completeIfPossible(context);
	}

	/**
	 * Will not work if this ScriptValue is a Double, it wont auto-cast Integer to Double
	 */
	public int asInteger() {
		return asInteger(null, null);
	}

	/**
	 * Will not work if this ScriptValue is a Double, it wont auto-cast Integer to Double
	 */
	public int asInteger(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof Integer) return (Integer) v;
		throw Incomplete.invalidType("Integer", this, where).completeIfPossible(context);
	}

	public boolean asBoolean() {
		return asBoolean(null, null);
	}

	public boolean asBoolean(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof Boolean) return (boolean) (Boolean) v;
		throw Incomplete.invalidType("Boolean", this, where).completeIfPossible(context);
	}

	public ScriptValueList<Object> asList() {
		return asList(null, null);
	}

	public ScriptValueList<Object> asList(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof ScriptValueList) return (ScriptValueList<Object>) v;
		throw Incomplete.invalidType("List", this, where).completeIfPossible(context);
	}

	public ScriptValueMap<Object, Object> asMap() {
		return asMap(null, null);
	}

	public ScriptValueMap<Object, Object> asMap(@Nullable Context context, @Nullable ScriptStackTraceElement where) {
		if (v instanceof ScriptValueMap) return (ScriptValueMap<Object, Object>) v;
		throw Incomplete.invalidType("Dictionary", this, where).completeIfPossible(context);
	}
}