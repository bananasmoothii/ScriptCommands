package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import static fr.bananasmoothii.scriptcommands.bukkit.ScriptCommands.logger;

public class UsableFunctions {
	
	public HashMap<String, ScriptValue<?>> volatileVariables = new HashMap<>();        // public, but use with caution
	public static HashMap<String, ScriptValue<?>> globalVariables = new HashMap<>();

	private static final ScriptValue<NoneType> NONE = new ScriptValue<>(null);
	
	public ScriptValue<String> getDisplayName(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 1, args);

		// will be spigot's real method of course
		String name = args.get(0).asString();
		if (name.equals("Bananasmoothii"))
			name = "[Fonda] Bananasmoothii";
		else if (name.equals("Dryter97")) {
			name = "[Admin] Dryter97";
		}
		return new ScriptValue<>(name);
	}

	/**
	 * Can take any parameter as it calls {@link Object#toString()} on it
	 */
	public ScriptValue<String> toText(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 1, args);

		return new ScriptValue<>(args.get(0).toString());
	}
	
	/* to copy/paste from here:|
	
	public ScriptValue (ArrayList<ScriptValue> args) {
		ScriptException.checkArgsNumber(, , args);
		
	}
	*/
	
	public ScriptValue<NoneType> player_run(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 2, args);
		
		ArrayList<String> playersInArg = new ArrayList<>();
		if (args.get(1).asList().size() == 0) {
			playersInArg.add(volatileVariables.get("player").asString());
		}
		else {
			for (ScriptValue<?> player: args.get(1).asList()) {
				playersInArg.add(player.asString());
			}
		}
		System.out.println("player " + String.join(", ", playersInArg) + " did: " + args.get(0).asString() + "\n");
		return NONE;
	}
	
	public ScriptValue<NoneType> player_msg(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 2, args);
		
		ArrayList<String> playersInArg = new ArrayList<>();
		if (args.get(1).asList().size() == 0) {
			playersInArg.add(volatileVariables.get("player").asString());
		}
		else {
			for (ScriptValue<?> player: args.get(1).asList()) {
				playersInArg.add(player.asString());
			}
		}
		System.out.println("player " + String.join(", ", playersInArg) + " has a message: " + args.get(0).asString() + "\n");
		return NONE;
	}

	public ScriptValue<NoneType> console_msg(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 2, args);
		logger.info(args.get(0).toString());
		return NONE;
	}
	
	public ScriptValue<String> join(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 2, args);
		
		if (args.size() == 1)
			args.add(new ScriptValue<>(""));
		ArrayList<String> list = new ArrayList<>();
		for (ScriptValue<?> value: args.get(0).asList()) {
			list.add(value.toString());
		}
		return new ScriptValue<>(String.join(args.get(1).asString(), list));
	}
	
	public ScriptValue<ScriptValueList<String>> split(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 2, args);
		
		if (args.size() == 1)
			args.add(new ScriptValue<>(""));
		ScriptValueList<String> list = new ScriptValueList<>();
		for (String string: args.get(0).asString().split(Pattern.quote(args.get(1).asString()))) {
			list.add(new ScriptValue<>(string));
		}
		return new ScriptValue<>(list);
	}
	
	public ScriptValue<?> papi(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 1, args);
		return args.get(0);
	}

	public ScriptValue<?> randChoice(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 1, args);

		Random random = new Random();
		return args.get(0).asList().get(random.nextInt(args.get(0).asList().size()));
	}

	public ScriptValue<ScriptValueList<Object>> range(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 3, args);
		boolean allInt = true;
		for (ScriptValue arg: args) {
			if (! arg.is("Integer")) {
				allInt = false;
				break;
			}
		}
		if (allInt) {
			int start, stop, step;
			if (args.size() == 1) {
				start = 0;
				stop = args.get(0).asInteger();
				step = 1;
			} else {
				start = args.get(0).asInteger();
				stop = args.get(1).asInteger();
				if (args.size() == 3)
					step = args.get(2).asInteger();
				else
					step = 1;
			}
			ScriptValueList<Object> list = new ScriptValueList<>();
			// start is used as current value
			while (start < stop) {
				list.add(new ScriptValue<>(start));
				start += step;
			}
			return new ScriptValue<>(list);
		}
		double start, stop, step;
		if (args.size() == 1) {
			start = 0.0;
			stop = args.get(0).asDouble();
			step = 1.0;
		} else {
			start = args.get(0).asDouble();
			stop = args.get(1).asDouble();
			if (args.size() == 3)
				step = args.get(2).asDouble();
			else
				step = 1.0;
		}
		ScriptValueList<Object> list = new ScriptValueList<>();
		// start is used as current value
		while (start < stop) {
			list.add(new ScriptValue<>(start));
			start += step;
		}
		return new ScriptValue<>(list);
	}

	public ScriptValue<Integer> toInteger(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(1, 1, args);
		ScriptValue<?> arg = args.get(0);
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
				return new ScriptValue<>((int) Math.round(arg.asDouble()));
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

	public ScriptValue<NoneType> add(ScriptValueList<Object> args) {
		ScriptException.checkArgsNumber(2, args);
		ScriptValueList<Object> list = args.get(0).asList();
		for (int i = 1; i < args.size(); i++) {
			list.add(args.get(i));
		}
		return NONE;
	}

	//TODO: do more functions, for example: eval, exec, function to modify lists/dictionaries and other more minecraft-related
}