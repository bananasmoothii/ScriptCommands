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
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.execution.Args.NamingPattern;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings({"unchecked", "unused", "MethodMayBeStatic"})
public class BaseUsableFunctions {

	private final Context context;

	private static final ScriptValue<NoneType> NONE = new ScriptValue<>(null);

	public BaseUsableFunctions(@NotNull Context context) {
		this.context = Objects.requireNonNull(context);
	}

	@ScriptFunctionMethod
	public ScriptValue<String> getDisplayName(Args args) {
		String name = args.getSingleArg().asString();
		// will be spigot's real method of course
		if (name.equals("Bananasmoothii"))
			name = "[Fonda] Bananasmoothii";
		else if (name.equals("Dryter97")) {
			name = "[Admin] Dryter97";
		}
		return new ScriptValue<>(name);
	}

	/**
	 * Can take any parameter as it calls {@link ScriptValue#toString()} on it
	 */
	@ScriptFunctionMethod
	public ScriptValue<String> toText(Args args) {
		String s = args.getSingleArg().toString();
		return new ScriptValue<>(s);
	}

	@NamingPatternProvider
	public static final NamingPattern player_cmd = new NamingPattern()
			.setNamingPattern("cmd", "player");


	@ScriptFunctionMethod
	public ScriptValue<NoneType> player_cmd(Args args) {
		args.setNamingPattern(new NamingPattern()
				.setNamingPattern(1, "cmd")
				.setNamingPattern("player", "cmd"));
		ScriptValue<Object> players = args.getArgIfExist("player");
		String cmd = args.getArg("cmd").asString();
		
		ArrayList<String> playersInArg = new ArrayList<>();
		if (players == null) {
			if (! context.normalVariables.containsKey("player") || ! context.normalVariables.get("player").is("String"))
				throw new ScriptException(ScriptException.ExceptionType.INVALID_ARGUMENTS, args, "no player was specified and couldn't get it from the normal variables.");
			playersInArg.add(context.normalVariables.get("player").asString());
		}
		else {
			for (ScriptValue<?> player: players.asList()) {
				playersInArg.add(player.asString());
			}
		}
		CustomLogger.finer("player " + String.join(", ", playersInArg) + " ran: " + cmd );
		return NONE;
	}

	@NamingPatternProvider
	public static final NamingPattern player_msg = new NamingPattern()
			.setNamingPattern("msg", "player");
	
	@ScriptFunctionMethod
	public ScriptValue<NoneType> player_msg(Args args) {
		ScriptValue<Object> players = args.getArgIfExist("player");
		String msg = args.getArg("msg").asString();
		
		ArrayList<String> playersInArg = new ArrayList<>();
		if (players == null) {
			playersInArg.add(context.normalVariables.get("player").asString());
		}
		else {
			for (ScriptValue<?> player: players.asList()) {
				playersInArg.add(player.asString());
			}
		}
		CustomLogger.finer("player " + String.join(", ", playersInArg) + " was sent a message: " + msg);
		return NONE;
	}

	@ScriptFunctionMethod
	public ScriptValue<NoneType> console_msg(Args args) {
		CustomLogger.info(args.getSingleArg().toString());
		return NONE;
	}
	
	@NamingPatternProvider
	public static final NamingPattern join = new NamingPattern()
			.setNamingPattern("list", "separator");

	@ScriptFunctionMethod
	public ScriptValue<String> join(Args args) {
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
	public ScriptValue<ScriptValueList<String>> split(Args args) {
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
				list.add(new ScriptValue<>(String.valueOf(string.charAt(i))));
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
							list.add(new ScriptValue<>(element.toString()));
						element.setLength(0);
						i += j - 1; // - 1 as the for will add 1
						continue;
					}
				}
				element.append(c);
			}
			if (element.length() != 0)
				list.add(new ScriptValue<>(element.toString()));
		}
		return new ScriptValue<>(list);
	}

	@ScriptFunctionMethod
	public ScriptValue<?> papi(Args args) {
		return args.getSingleArg();
	}

	@ScriptFunctionMethod
	public ScriptValue<?> randChoice(Args args) {

		Random random = new Random();
		ScriptValueList<Object> list = args.getSingleArg().asList();
		return list.get(random.nextInt(list.size()));
	}

	public static final NamingPattern range = new NamingPattern()
			.setNamingPattern(1, "stop")
			.setNamingPattern("start", "stop", "step")
			.setDefaultValue("start", 0)
			.setDefaultValue("step", 1);

	@ScriptFunctionMethod
	public ScriptValue<ScriptValueList<Object>> range(Args args) {
		ScriptValue<?> start = args.getArg("start"),
				stop  = args.getArg("stop"),
				step  = args.getArg("step");

		boolean allInt = start.is("Integer") && stop.is("Integer") && step.is("Integer");
		if (allInt) {
			int start1 = start.asInteger(),
					stop1 = stop.asInteger(),
					step1 = step.asInteger();
			ScriptValueList<Object> list = new ScriptValueList<>();
			// start is used as current value
			while (start1 < stop1) {
				list.add(new ScriptValue<>(start1));
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
			list.add(new ScriptValue<>(start));
			start1 += step1;
		}
		return new ScriptValue<>(list);
	}

	@ScriptFunctionMethod
	public ScriptValue<Integer> toInteger(Args args) {
		ScriptValue<?> arg = args.getSingleArg();
		switch (arg.type) {
			case "Text":
				try {
					return new ScriptValue<>(Integer.valueOf(arg.toString().replace("_", "")));
				} catch (NumberFormatException ignored) {
					throw new ScriptException(ScriptException.ExceptionType.CONVERSION_ERROR,
							StackTraceUtils.getFromStackTrace(-1), args, "Cannot convert the Text \"" +
							arg.asString() + "\" to Integer.");
				}
			case "Integer":
				return (ScriptValue<Integer>) arg;
			case "Decimal":
				return new ScriptValue<>(arg.asInteger());
			case "Boolean":
				if (arg.asBoolean())
					return new ScriptValue<>(1);
				else
					return new ScriptValue<>(0);
			default:
				throw new ScriptException(ScriptException.ExceptionType.CONVERSION_ERROR,
						StackTraceUtils.getFromStackTrace(-1), args, "Cannot convert " + arg.type + " to Integer.");
		}
	}

	@NamingPatternProvider
	public static final NamingPattern add = new NamingPattern()
			.setNamingPattern("list");

	@ScriptFunctionMethod
	public ScriptValue<NoneType> add(Args args) {
		ScriptValueList<Object> list = args.getArg("list").asList();
		ScriptValueList<Object> remainingArgs = args.getRemainingArgsList();
		list.addAll(remainingArgs.clone());
		return NONE;
	}

	//TODO: do more functions, for example: eval, exec, function to modify lists/dictionaries and other more minecraft-related
}