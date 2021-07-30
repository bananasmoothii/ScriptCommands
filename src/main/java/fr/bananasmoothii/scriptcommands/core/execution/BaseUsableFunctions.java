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

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.execution.Args.NamingPattern;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ExceptionType;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static fr.bananasmoothii.scriptcommands.core.execution.ScriptValue.NONE;

@SuppressWarnings({"unchecked", "unused"})
public class BaseUsableFunctions {

	/**
	 * Can take any parameter as it calls {@link ScriptValue#toString()} on it
	 */
	@ScriptFunctionMethod
	public static ScriptValue<String> toText(Args args) {
		String s = args.getSingleArg().toString();
		return new ScriptValue<>(s);
	}

	@ScriptFunctionMethod
	public static ScriptValue<NoneType> console_msg(Args args) {
		CustomLogger.info(args.getSingleArg().toString());
		return NONE;
	}
	
	@NamingPatternProvider
	public static final NamingPattern join = new NamingPattern()
			.setNamingPattern("list", "separator");

	@ScriptFunctionMethod
	public static ScriptValue<String> join(Args args) {
		ScriptValueList<?> list = args.getArg("list").asList();
		String separator = args.getArg("separator").asString();

		ArrayList<String> strings = new ArrayList<>();
		for (ScriptValue<?> value: list) {
			strings.add(value.toString());
		}
		return new ScriptValue<>(String.join(separator, strings));
	}

	@NamingPatternProvider
	public static final NamingPattern split = new NamingPattern()
			.setNamingPattern("string", "separator");
	
	@ScriptFunctionMethod
	public static ScriptValue<ScriptValueList<String>> split(Args args) {
		String string = args.getArg("string").asString();
		int length = string.length();
		ScriptValue<Object> scriptValueSeparator = args.getArgIfExist("separator");
		String separator;
		if (scriptValueSeparator != null)
			separator = scriptValueSeparator.asString();
		else
			separator = "";

		ScriptValueList<String> list = new ScriptValueList<>();
		if (separator.length() == 0) {
			for (int i = 0; i < length; i++) {
				list.add(new ScriptValue<>(String.valueOf(string.charAt(i))), args.context);
			}
		} else {
			char c;
			char firstSepChar = separator.charAt(0);
			StringBuilder element = new StringBuilder();
			for (int i = 0; i < length; i++) {
				c = string.charAt(i);
				if (c == firstSepChar) {
					boolean allMatch = true;
					int j = 1;
					for (; j < separator.length() && i + j < length; j++) {
						if (string.charAt(i + j) != separator.charAt(j)) {
							allMatch = false;
							break;
						}
					}
					if (allMatch) {
						if (element.length() != 0)
							list.add(new ScriptValue<>(element.toString()), args.context);
						element.setLength(0);
						i += j - 1; // - 1 as the for will add 1
						continue;
					}
				}
				element.append(c);
			}
			if (element.length() != 0)
				list.add(new ScriptValue<>(element.toString()), args.context);
		}
		return new ScriptValue<>(list);
	}

	@ScriptFunctionMethod
	public static ScriptValue<?> papi(Args args) {
		return new ScriptValue<>("papi: " + args.getSingleArg());
	}

	@ScriptFunctionMethod
	public static ScriptValue<?> randChoice(Args args) {
		ScriptValueList<Object> list = args.getSingleArg().asList();
		return list.get(ThreadLocalRandom.current().nextInt(list.size(args.context)), args.context);
	}

	@NamingPatternProvider
	public static final NamingPattern range = new NamingPattern()
			.setNamingPattern(1, "stop")
			.setNamingPattern("start", "stop", "step")
			.setDefaultValue("start", 0)
			.setDefaultValue("step", 1);

	@ScriptFunctionMethod
	public static ScriptValue<ScriptValueList<Object>> range(Args args) {
		ScriptValue<?> start = args.getArg("start"),
				stop  = args.getArg("stop"),
				step  = args.getArg("step");

		boolean allInt = start.is(ScriptValue.ScriptValueType.INTEGER)
				&& stop.is(ScriptValue.ScriptValueType.INTEGER)
				&& step.is(ScriptValue.ScriptValueType.INTEGER);
		if (allInt) {
			int start1 = start.asInteger(),
					stop1 = stop.asInteger(),
					step1 = step.asInteger();
			ScriptValueList<Object> list = new ScriptValueList<>();
			// start is used as current value
			while (start1 < stop1) {
				list.add(new ScriptValue<>(start1), args.context);
				start1 += step1;
			}
			return new ScriptValue<>(list);
		}
		double start1 = start.asDouble(),
				stop1 = stop.asDouble(),
				step1 = step.asDouble();
		ScriptValueList<Object> list = new ScriptValueList<>();
		// start is used as current value
		while (start1 < stop1) {
			list.add(new ScriptValue<>(start), args.context);
			start1 += step1;
		}
		return new ScriptValue<>(list);
	}

	@ScriptFunctionMethod
	public static ScriptValue<Integer> toInteger(Args args) {
		ScriptValue<?> arg = args.getSingleArg();
		switch (arg.type) {
			case TEXT:
				try {
					return new ScriptValue<>(Integer.valueOf(arg.toString().replace("_", "")));
				} catch (NumberFormatException e) {
					throw new ScriptException(ExceptionType.CONVERSION_ERROR, args.context,
							"Cannot convert the Text \"" + arg.asString() + "\" to Integer.");
				}
			case INTEGER:
				return (ScriptValue<Integer>) arg;
			case DECIMAL:
				return new ScriptValue<>((int) arg.asDouble());
			case BOOLEAN:
				if (arg.asBoolean())
					return new ScriptValue<>(1);
				else
					return new ScriptValue<>(0);
			default:
				throw new ScriptException(ExceptionType.CONVERSION_ERROR, args.context,
						"Cannot convert " + Types.getPrettyArgAndType(arg) + " to Integer.");
		}
	}

	@NamingPatternProvider
	public static final NamingPattern add = new NamingPattern()
			.setNamingPattern("list");

	@ScriptFunctionMethod
	public static ScriptValue<NoneType> add(Args args) {
		ScriptValueList<Object> list = args.getArg("list").asList();
		ScriptValueList<Object> remainingArgs = args.getRemainingArgsList();
		list.addAll(remainingArgs.clone());
		return NONE;
	}

	//TODO: do more functions, for example: eval, exec, function to modify lists/dictionaries and other more minecraft-related
}