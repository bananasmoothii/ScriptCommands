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
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.*;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ExceptionType;
import org.antlr.v4.runtime.misc.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

/**
 * Used for Scripts
 */
@SuppressWarnings("unchecked")
public class Context implements Cloneable {

	// here are saved every user variables in the scripts.
	// these are not final to allow lots of modulations, please don't mess with that XD
	public StringScriptValueMap<Object> normalVariables = new StringScriptValueMap<>();
	public static StringScriptValueMap<Object> globalVariables = StringScriptValueMap.getTheGlobal();

	protected static HashMap<String, Pair<ScriptFunction, Args.NamingPattern>> scriptFunctions = new HashMap<>();
	protected static HashMap<String, Pair<ScriptIterator, Args.NamingPattern>> scriptIterators = new HashMap<>();

	static {
		registerMethodsFromClass(BaseUsableFunctions.class);
		registerMethodsFromClass(BaseUsableIterators.class);
	}

	/**
	 * @param baseVariables variables that will be given to the script, null for no other variable
	 */
	public Context(@Nullable StringScriptValueMap<Object> baseVariables) {
		if (baseVariables != null)
			normalVariables.putAll(baseVariables);
	}

	public Context() {
		this(null);
	}

	public static void registerMethodsFromClass(Class<?> cls) {
		for (Method method: cls.getDeclaredMethods()) {
			boolean isIterator;
			if (method.isAnnotationPresent(ScriptFunctionMethod.class))
				isIterator = false;
			else if (method.isAnnotationPresent(ScriptIteratorMethod.class))
				isIterator = true;
			else continue;

			if (! Arrays.equals(new Class[] {Args.class}, method.getParameterTypes()))
				throw new ScriptFunction.InvalidMethodException("method's type isn't Args.");

			if (!isIterator && ! method.getReturnType().equals(ScriptValue.class))
				throw new ScriptFunction.InvalidMethodException("method's return type isn't ScriptValue.");
			if (isIterator && ! method.getReturnType().equals(Iterator.class))
				throw new ScriptFunction.InvalidMethodException("method's return type isn't ScriptValue.");

			String variable = method.getName();

			@Nullable Args.NamingPattern namingPattern = null;
			try {
				Field namingPatternProvider = cls.getField(variable);
				if (namingPatternProvider.isAnnotationPresent(NamingPatternProvider.class)
						&& namingPatternProvider.getType().equals(Args.NamingPattern.class)) {
					namingPattern = (Args.NamingPattern) namingPatternProvider.get(null);
				}
			} catch (NoSuchFieldException ignore) { }
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			if (! isIterator) {
				registerScriptFunction(variable, (Args args) -> {
					try {
						return (ScriptValue<Object>) method.invoke(null, args);

					} catch (IllegalAccessException e) {
						throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
								"Access to function \"" + variable + "\" was refused");
					} catch (InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						if (targetException instanceof ScriptException) {
							throw (ScriptException) targetException;
						} else {
							throw (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
									"other error:\n" + targetException.getClass().getName() + ": " + targetException.getMessage())
									.initCause(targetException);
						}
					}
				}, namingPattern);
			} else {
				registerScriptIterator(variable, (Args args) -> {
					try {
						return (Iterator<ScriptValue<?>>) method.invoke(null, args);

					} catch (IllegalAccessException e) {
						throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
								"Access to function \"" + variable + "\" was refused");
					} catch (InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						if (targetException instanceof ScriptException) {
							throw (ScriptException) targetException;
						} else {
							throw (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
									"other error:\n" + targetException.getClass().getName() + ": " + targetException.getMessage())
									.initCause(targetException);
						}
					}
				}, namingPattern);
			}
		}
	}

	public static void registerScriptFunction(String funcName, ScriptFunction scriptFunction, @Nullable Args.NamingPattern namingPattern) {
		scriptFunctions.put(funcName, new Pair<>(scriptFunction, namingPattern));
	}

	public static void registerScriptIterator(String funcName, ScriptIterator scriptIterator, @Nullable Args.NamingPattern namingPattern) {
		scriptIterators.put(funcName, new Pair<>(scriptIterator, namingPattern));
	}

	public ScriptValue<?> callAndRun(String variable)  {
		return this.callAndRun(variable, new Args(this));
	}

	public ScriptValue<?> callAndRun(String variable, Args args) {
		ScriptValue<?> value;

		ScriptsParser.StartContext funcStartContext;
		try {
			//noinspection ConstantConditions // for Intllij Idea saying the statement below could create a NullPointerException, but we are catching it anyway
			funcStartContext = Config.getCorrespondingContainingScripts(ContainingScripts.Type.FUNCTION, variable).parseTree;
			ScriptsExecutor visitor = new ScriptsExecutor(new Context(null)); // baseVariables are not copied over.
			visitor.visit(funcStartContext);
			return visitor.getReturned();
		}
		catch (NullPointerException ignored) { }

		if (scriptFunctions.containsKey(variable)) {
			Pair<ScriptFunction, Args.NamingPattern> pair = scriptFunctions.get(variable);
			return pair.a.run(args.setNamingPattern(pair.b));
		}

		value = normalVariables.get(variable);
		if (value != null) {
			ScriptException.shouldHaveNoArgs(variable, args);
			return value;
		}

		value = globalVariables.get(variable);
		if (value != null) {
			ScriptException.shouldHaveNoArgs(variable, args);
			return value;
		}

		throw new ScriptException(ExceptionType.NOT_DEFINED, variable, args, '\"' + variable + "\" does not exist.");
	}

	public static @Nullable Iterator<ScriptValue<?>> getIterator(String name, Args args) {
		if (scriptIterators.containsKey(name)) {
			Pair<ScriptIterator, Args.NamingPattern> pair = scriptIterators.get(name);
			return pair.a.run(args.setNamingPattern(pair.b));
		}
		return null;
	}
	 
	public void assign(String key, ScriptValue<?> value, boolean global) {
		if (scriptFunctions.containsKey(key) || (global && normalVariables.containsKey(key))) {
			throw new ScriptException(ExceptionType.NOT_OVERRIDABLE, "<assignment to " + key + '>', "",
					"you tried to create/modify \"" + key + "\", but it is already a default function, or not a global" +
							" variable and you tried to make it global, that cannot be overridden.");
		}
		if (global || globalVariables.containsKey(key)) {
			globalVariables.put(key, (ScriptValue<Object>) value);
		}
		else {
			normalVariables.put(key, (ScriptValue<Object>) value);
		}
	}
	//public void assign(String key, Object value, boolean global) {this.assign(key, new ScriptValue(value), global);}

	public void delete(String key) {
		normalVariables.remove(key);
		globalVariables.remove(key);
	}

	public static ScriptValue<?> trigger(ContainingScripts.Type scriptType, String scriptName, @Nullable StringScriptValueMap<Object> baseVariables) {
		CustomLogger.finer("running " + scriptType.name() + " " + scriptName + "(" + Types.getPrettyArgs(null, baseVariables) + ")");
		ScriptsParser.StartContext parseTree = Objects.requireNonNull(
				Config.getCorrespondingContainingScripts(scriptType, scriptName), scriptName + " doesn't exist as " + scriptType.name())
				.parseTree;
		ScriptsExecutor visitor = new ScriptsExecutor(new Context(baseVariables));
		visitor.visit(parseTree);
		return visitor.getReturned();
	}

	public static ScriptThread threadTrigger(ContainingScripts.Type scriptType, String scriptName, @Nullable StringScriptValueMap<Object> baseVariables) {
		CustomLogger.finer("running " + scriptType.name() + " " + scriptName + "(" + Types.getPrettyArgs(null, baseVariables) + ") ASYNCHRONOUSLY");
		ScriptsParser.StartContext parseTree = Objects.requireNonNull(
				Config.getCorrespondingContainingScripts(scriptType, scriptName), scriptName + " doesn't exist as " + scriptType.name())
				.parseTree;
		ScriptThread scriptThread = new ScriptThread(new Context(baseVariables), parseTree);
		scriptThread.start();
		return scriptThread;
	}

	@Override
	public Context clone() {
		try {
			return (Context) super.clone(); // it should work, even with the volatile variables in the UsableFunction
		} catch (CloneNotSupportedException e) {
			throw ScriptException.toScriptException(e);
		}
	}
}