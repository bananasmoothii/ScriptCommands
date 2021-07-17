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

import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * This class contains only static methods used to switch easier between
 * ArrayLists of {@link ScriptValue} and Strings
 */
@SuppressWarnings("unchecked")
public class Types {
	
	//protected static Pattern patternType = Pattern.compile("^(\\[*)[^\\(]+\\.([^\\.\\(]+)(?:\\(.*\\))?$");
	
	private Types() { }

	/**
	 * Transforms an ArrayList of {@link ScriptValue} into something
	 * like "{@code 12, ["a", 4], [=]}" using {@link Types#getPrettyArg(ScriptValue)}
	 * @see Types#getPrettyTypes(ScriptValueList)
	 */
	public static String getPrettyArgs(ScriptValueList<?> args) {
		ArrayList<String> formattedArgs = new ArrayList<>();
		for (ScriptValue<?> arg: args) {
			formattedArgs.add(getPrettyArg(arg));
		}
		return String.join(", ", formattedArgs);
	}

	public static String getPrettyArgs(@Nullable ScriptValueList<Object> args, @Nullable StringScriptValueMap<Object> kwargs) {
		ArrayList<String> formattedArgs = new ArrayList<>();
		if (kwargs != null && kwargs.size() != 0) {
			for (Entry<String, ScriptValue<Object>> entry : kwargs.entrySet()) {
				formattedArgs.add(entry.getKey() + " = " + getPrettyArg(entry.getValue()));
			}
			if (args != null && args.size() != 0) {
				return getPrettyArgs(args) + ", " + String.join(", ", formattedArgs);
			}
			return String.join(", ", formattedArgs);
		}
		if (args != null && args.size() != 0) {
			return getPrettyArgs(args);
		}
		return "";
	}

	/**
	 * This doesn't used default toString() methods, because for example, a list of
	 * strings will result in {@code [a, b, c]} and not in {@code ["a", "b", "c"]} as excepted.
	 * @see Types#getPrettyArgs(ScriptValueList)
	 */
	public static String getPrettyArg(ScriptValue<?> arg) {
		switch (arg.type) {
			case TEXT:
				return "\"" + arg.asString().replace("\"", "\"\"") + "\"";
			case LIST:
				return "[" + getPrettyArgs(arg.asList()) + "]";
			case DICTIONARY:
				if (arg.asMap().size() != 0) {
					ArrayList<String> keyValues = new ArrayList<>();
					for (Object entryObj : arg.asMap().entrySet()) {
						Entry<ScriptValue<?>, ScriptValue<?>> entry = (Entry<ScriptValue<?>, ScriptValue<?>>) entryObj;
						keyValues.add(getPrettyArg(entry.getKey()) + "=" + getPrettyArg(entry.getValue()));
					}
					return "[" + String.join(", ", keyValues) + "]";
				}
				return "[=]";
			default:
				return arg.v.toString();
		}
	}

	/**
	 * Transforms an ArrayList of {@link ScriptValue} into a
	 * String using the {@link ScriptValue#type} field
	 * @see .Types#getPrettyArgs(ScriptValueList)
	 */
	public static String getPrettyTypes(ScriptValueList<?> got) {
		ArrayList<String> types = new ArrayList<>();
		for (ScriptValue<?> value: got) {
			types.add(value.type.name);
		}
		return String.join(", ", types);
	}

	public static String getPrettyArgAndType(ScriptValue<?> arg) {
		return getPrettyArg(arg) + '<' + arg.type.name + '>';
	}

	public static String getPrettyArgAndType(ScriptValueList<?> args) {
		StringBuilder sb = new StringBuilder(args.size() * 10);
		for (ScriptValue<?> arg : args) {
			sb.append(getPrettyArgAndType(arg)).append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}
	
}