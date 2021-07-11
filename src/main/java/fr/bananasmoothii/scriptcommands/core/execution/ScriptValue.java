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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Value for the Scripts. Each variable in the Scripts are in fact a ScriptValue.
 * @see ScriptValue#ScriptValue(Object) the constructor
 */
@SuppressWarnings("unchecked")
public class ScriptValue<T> implements Cloneable, Iterable<ScriptValue<?>> {
	
	/**
	* The value this class is made for
	*/
	@NotNull
	public final T v;

	public final SVType type;

	private static final NoneType NONE = NoneType.INSTANCE;
	
	/**
	 * Value for the Scripts. Each variable in the Scripts are in fact a ScriptValue.
	 * @param obj of type T. Possible types are:
	 *     <ul>
	 *         <li>{@code null}, it will be converted to {@link NoneType}</li>
	 *         <li>{@link NoneType}</li>
	 *         <li>{@link Integer}</li>
	 *         <li>{@link Double}</li>
	 *         <li>{@link String}</li>
	 *         <li>{@link Boolean}</li>
	 *         <li>{@link ScriptValueList}{@literal <ScriptValue>}</li>
	 *         <li>{@link ScriptValueMap}{@literal <ScriptValue, ScriptValue>}</li>
	 *         <li>{@link List}{@literal <Object>}</li> will be automatically converted to a {@link ScriptValueList}
	 *         <li>{@link Map}{@literal <Object, Object>}</li> will be automatically converted to a {@link ScriptValueMap}
	 *     </ul>
	 */
	public ScriptValue(@Nullable T obj) {

		v = obj == null ? (T) NONE : obj;
		if (obj == null || obj instanceof NoneType) type = SVType.NONE;
		else if (obj instanceof Integer)            type = SVType.INTEGER;
		else if (obj instanceof Double)             type = SVType.DECIMAL;
		else if (obj instanceof String)             type = SVType.TEXT;
		else if (obj instanceof Boolean)            type = SVType.BOOLEAN;
		else if (obj instanceof ScriptValueList)    type = SVType.LIST;
		else if (obj instanceof ScriptValueMap)     type = SVType.DICTIONARY;
		else throw new IllegalArgumentException(obj.getClass().getName()+
					" is not a valid argument, must be String, Integer, Double, " +
					"Boolean, ScriptValueList or ScriptValueMap .");
	}

	public enum SVType {
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

		SVType(String name, byte asByte) {
			this.name = name;
			this.asByte = asByte;
		}
	}

	/**
	 * Contrary of {@link #toNormalClass(boolean)}</br>
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
		if (v instanceof ScriptValueCollection) return ((ScriptValueCollection) v).toNormalClasses(forJson);
		else if (is(SVType.NONE)) return null;
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
		throw new IllegalArgumentException("the passed String (\"" + s + "\") cannot be converted to Integer, Decimal or Boolean.");
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

	public boolean is(SVType type) {
		return this.type == type;
	}
	
	public boolean isNumber() {
		return is(SVType.INTEGER) || is(SVType.DECIMAL);
	}

	@NotNull
	@Override
	public Iterator<ScriptValue<?>> iterator() {
		return iterator("<unknown location>");
	}
	
	public Iterator<ScriptValue<?>> iterator(String where) {
		if (is(SVType.LIST)) {
			return ((ScriptValueList) v).iterator(); // idk what to do about this raw type
		}
		if (is(SVType.TEXT)) {
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
		if (is(SVType.DICTIONARY)) {
			return new Iterator<ScriptValue<?>>() {
				private final Iterator<Entry<ScriptValue<Object>, ScriptValue<Object>>> iterator = ((ScriptValueMap<Object, Object>) v).entrySet().iterator();

				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public ScriptValue<?> next() {
					ScriptValueList<Object> pair = new ScriptValueList<>();
					Entry<ScriptValue<Object>, ScriptValue<Object>> next = iterator.next();
					pair.add(next.getKey());
					pair.add(next.getValue());
					return new ScriptValue<>(pair);
				}
			};
		}
		throw ScriptException.invalidType("[iteration] " + where, "List, Text or Dictionary", where.replaceAll("^.*?\\s(\\S+)$", "\\1"), type);
	}

	public ScriptValueList<Object> iterableList(String where) {
		Iterator<ScriptValue<?>> iterator = iterator(where);
		ScriptValueList<Object> list = new ScriptValueList<>();
		while (iterator.hasNext()) {
			list.add((ScriptValue<Object>) iterator.next());
		}
		return list;
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
		if (is(SVType.TEXT))
			return (String) v;
		return Types.getPrettyArg(this);
	}
	
	/**
	 * Not tge same as {@link ScriptValue#toString()} because this can throw an error.
	 */
	public String asString() {return this.asString(1);}
	public String asString(int callingFuncDepth) {
		try {
			return (String) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "Text", v.toString(), type);
		}
	}
	
	public Double asDouble() {return this.asDouble(1);}
	public Double asDouble(int callingFuncDepth) {
		try {
			return (Double) v;
		}
		catch (ClassCastException e) {
			try {
				return ((Integer) v).doubleValue();
			}
			catch (ClassCastException ignored) { }
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "Decimal or Integer", v.toString(), type);
		}
	}
	
	public Integer asInteger() {return this.asInteger(1);}
	public Integer asInteger(int callingFuncDepth) {
		try {
			return (Integer) v;
		}
		catch (ClassCastException e) {
			try {
				return ((Double) v).intValue();
			}
			catch (ClassCastException ignored) { }
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "Integer", v.toString(), type);
		}
	}
	
	public Boolean asBoolean() {return this.asBoolean(1);}
	public Boolean asBoolean(int callingFuncDepth) {
		try {
			return (Boolean) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "Boolean", v.toString(), type);
		}
	}

	public ScriptValueList<Object> asList() {return this.asList(1);}
	public ScriptValueList<Object> asList(int callingFuncDepth) {
		try {
			return (ScriptValueList<Object>) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "List", v.toString(), type);
		}
	}

	public ScriptValueMap<Object, Object> asMap() {return this.asMap(1);}
	public ScriptValueMap<Object, Object> asMap(int callingFuncDepth) {
		try {
			return (ScriptValueMap<Object, Object>) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth + 1), "Dictionary", v.toString(), type);
		}
	}
}