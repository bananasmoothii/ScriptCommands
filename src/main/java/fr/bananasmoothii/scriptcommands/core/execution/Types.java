package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueList;

import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * This class contains only static methods used to switch easier between
 * ArrayLists of {@link ScriptValue} and Strings
 */
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

	/**
	 * @see Types#getPrettyArgs(ScriptValueList)
	 */
	public static String getPrettyArg(ScriptValue<?> arg) {
		switch (arg.type) {
			case "Text":
				return "\"" + arg.asString().replace("\"", "\"\"") + "\"";
			case "List":
				return "[" + getPrettyArgs(arg.asList()) + "]";
			case "Dictionary":
				if (arg.asMap().size() == 0) {
					ArrayList<String> keyValues = new ArrayList<>();
					for (Object entryObj : arg.asMap().entrySet()) {
						Entry<ScriptValue<?>, ScriptValue<?>> entry = (Entry<ScriptValue<?>, ScriptValue<?>>) entryObj;
						keyValues.add(getPrettyArg(new ScriptValue<>(entry.getKey())) + "=" + getPrettyArg(new ScriptValue<>(entry.getValue())));
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
			types.add(value.type);
		}
		return String.join(", ", types);
	}
	
}