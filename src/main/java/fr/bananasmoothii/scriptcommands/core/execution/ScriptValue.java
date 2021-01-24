package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Value for the Scripts. Each variable in the Scripts are in fact a ScriptValue.
 * @see ScriptValue#ScriptValue(Object) the constructor
 */
public class ScriptValue<T> implements Cloneable {
	
	/**
	* The value this class is made for
	*/
	public final T v;

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
	public final String type;

	/**
	 * Will be:
	 * <ul>
	 *     <li>0 for None</li>
	 *     <li>1 for Integer</li>
	 *     <li>2 for Decimal</li>
	 *     <li>3 for Boolean</li>
	 *     <li>4 for Text</li>
	 *     <li>5 for List</li>
	 *     <li>6 forDictionary</li>
	 * </ul>
	 */
	public final byte typeByte;

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
	 *         <li>{@link ArrayList}{@literal <ScriptValue>}</li>
	 *         <li>{@link HashMap}{@literal <ScriptValue, ScriptValue>}</li>
	 *     </ul>
	 */

	public ScriptValue(@Nullable T obj) {
		
		if (obj == null || obj instanceof NoneType) {
			v = (T) NONE;
			type = "None";
			typeByte = 0;
		}
		else {
			v = obj;
			if (obj instanceof Integer) {
				type = "Integer";
				typeByte = 1;
			} else if (obj instanceof Double) {
				type = "Decimal";
				typeByte = 2;
			} else if (obj instanceof String) {
				type = "Text";
				typeByte = 3;
			} else if (obj instanceof Boolean) {
				type = "Boolean";
				typeByte = 4;
			} else if (obj instanceof ScriptValueList) {
				type = "List";
				typeByte = 5;
			} else if (obj instanceof ScriptValueMap) {
				type = "Dictionary";
				typeByte = 6;
			} else throw new IllegalArgumentException(obj.getClass().getName()+
						" is not a valid argument, must be String, Integer, Double, " +
						"Boolean, ScriptValueList or ScriptValueMap .");
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof ScriptValue))
			return false;
		return this.v.equals(((ScriptValue<?>)o).v);
	}
	
	public boolean is(String type) {
		return this.type.equals(type);
	}
	
	public boolean isNumber() {
		return is("Integer") || is("Decimal");
	}
	
	public ArrayList<ScriptValue<Object>> iterableList(String where) {
		if (is("List")) {
			return ((ScriptValueList<Object>) v).toNormalClasses();
		}
		if (is("Text")) {
			String s = (String) v;
			ArrayList<ScriptValue<Object>> list = new ArrayList<>();
			for (int i = 0; i < s.length(); i++) {
				list.add(new ScriptValue<>(String.valueOf(s.charAt(i))));
			}
			return list;
		}
		if (is("Dictionary")) {
			ArrayList<ScriptValue<Object>> list = new ArrayList<>();
			for (Entry<ScriptValue<Object>, ScriptValue<Object>> entry: ((ScriptValueMap<Object, Object>) v).entrySet()) {
				ArrayList<ScriptValue<?>> pair = new ArrayList<>();
				pair.add(entry.getKey());
				pair.add(entry.getValue());
				list.add(new ScriptValue<>(pair));
			}
			return list;
		}
		throw ScriptException.invalidType("[iteration] " + where, "List, Text or Dictionary", where.replaceAll("^.*?\\s(\\S+)$", "\\1"), type);
		
	}

	@Override
	public ScriptValue<T> clone() {
		switch (type) {
			case "List":
				return new ScriptValue<>((T) ((ScriptValueList<?>) v).clone());
			case "Dictionary":
				return new ScriptValue<>((T)((ScriptValueMap<?, ?>) v).clone());
			default:
				return this;
		}
	}

	
	public Class<?> getValueClass() {
		return v.getClass();
	}

	@Override
	public String toString() {
		if (is("Text"))
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
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "Text", v.toString(), type);
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
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "Decimal or Integer", v.toString(), type);
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
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "Integer", v.toString(), type);
		}
	}
	
	public Boolean asBoolean() {return this.asBoolean(1);}
	public Boolean asBoolean(int callingFuncDepth) {
		try {
			return (Boolean) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "Boolean", v.toString(), type);
		}
	}

	public ScriptValueList<Object> asList() {return this.asList(1);}
	public ScriptValueList<Object> asList(int callingFuncDepth) {
		try {
			return (ScriptValueList<Object>) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "List", v.toString(), type);
		}
	}

	public ScriptValueMap<Object, Object> asMap() {return this.asMap(1);}
	public ScriptValueMap<Object, Object> asMap(int callingFuncDepth) {
		try {
			return (ScriptValueMap<Object, Object>) v;
		}
		catch (ClassCastException e) {
			throw ScriptException.invalidType(StackTraceUtils.getFromStackTrace(callingFuncDepth), "Dictionary", v.toString(), type);
		}
	}
}