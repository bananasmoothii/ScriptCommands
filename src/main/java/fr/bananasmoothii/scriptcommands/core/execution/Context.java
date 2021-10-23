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
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.Parsing;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.Config;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ContainingScripts.Type;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ContextStackTraceElement;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ScriptStackTraceElement;
import fr.bananasmoothii.scriptcommands.core.functions.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Used for Scripts. This is not thread-safe.
 */
@SuppressWarnings("unchecked")
public class Context implements Cloneable {

	// here are saved every user variables in the scripts.
	// these are not final to allow lots of modulations, please don't mess with that XD
	public StringScriptValueMap<Object> normalVariables = new StringScriptValueMap<>();
	public static StringScriptValueMap<Object> globalVariables = StringScriptValueMap.getTheGlobal();
	/** the player that triggered this command or event */
	public final @Nullable Player triggeringPlayer;

	protected @Nullable Context parent;
	/**
	 * The last call to {@link #callAndRun(String, Args, int, int)} or any similar overloaded method. If this is not null,
	 * it means there is a call to {@link #callAndRun(String, Args, int, int)} running.
	 */
	private @Nullable ScriptStackTraceElement lastCall;
	public final @Nullable String scriptName;
	public final @Nullable Type scriptType;

	protected static HashMap<String, Pair<ScriptFunction, Args.NamingPattern>> scriptFunctions = new HashMap<>();
	protected static HashMap<String, Pair<ScriptIterator, Args.NamingPattern>> scriptIterators = new HashMap<>();

	static {
		registerMethodsFromClass(BaseUsableFunctions.class);
		registerMethodsFromClass(BaseUsableIterators.class);
	}

	/**  
	 * @param scriptName the name of the script, e.g. "server_start"
	 * @param scriptType the type of the script 
	 */
	public Context(@Nullable String scriptName, @Nullable Type scriptType) {
		this(scriptName, scriptType, null);
	}

	/**
	 * @param scriptName the name of the script, e.g. "server_start"
	 * @param scriptType the type of the script
	 * @param parent if this context was created from the call of a function in the script (the stack trace is deeper)
	 */
	public Context(@Nullable String scriptName, @Nullable Type scriptType, @Nullable Context parent) {
		this(scriptName, scriptType, null, null, parent);
	}

	/**
	 * @param scriptName the name of the script, e.g. "server_start"
	 * @param scriptType the type of the script
	 * @param baseVariables variables that will be given to the script, null for no other variable
	 */
	public Context(@Nullable String scriptName, @Nullable Type scriptType, @Nullable Map<String, ScriptValue<Object>> baseVariables,
				   @Nullable Player triggeringPlayer) {
		this(scriptName, scriptType, baseVariables, triggeringPlayer, null);
	}

	/**
	 * @param scriptName the name of the script, e.g. "server_start"
	 * @param scriptType the type of the script
	 * @param baseVariables variables that will be given to the script, null for no other variable
	 * @param triggeringPlayer the player that triggered this command or event
	 * @param parent if this context was created from the call of a function in the script (the stack trace is deeper)
	 */
	public Context(@Nullable String scriptName, @Nullable Type scriptType, @Nullable Map<String, ScriptValue<Object>> baseVariables,
				   @Nullable Player triggeringPlayer, @Nullable Context parent) {
		this.scriptName = scriptName;
		this.scriptType = scriptType;
		if (baseVariables != null)
			normalVariables.putAll(baseVariables);
		this.triggeringPlayer = triggeringPlayer;
		this.parent = parent;
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


			if (! Modifier.isStatic(method.getModifiers()))
				throw new NullPointerException("The method " + variable + " in " + cls +
						" is annotated as @ScriptFunctionMethod or @ScriptIteratorMethod, but it is not static.");

			@Nullable Args.NamingPattern namingPattern = null;
			try {
				Field namingPatternProvider = cls.getField(variable);
				if (namingPatternProvider.isAnnotationPresent(NamingPatternProvider.class)
						&& namingPatternProvider.getType().equals(Args.NamingPattern.class)) {
					if (! Modifier.isStatic(namingPatternProvider.getModifiers()))
						throw new NullPointerException("The field " + variable + " in " + cls + " is annotated as @NamingPatternProvider, but it is not static.");
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
						throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, args.context,
								"Access to function \"" + variable + "\" was refused");
					} catch (InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						if (targetException instanceof ScriptException) {
							throw (ScriptException) targetException;
						} else {
							throw (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, args.context,
									"Other error:\n" + targetException.getClass().getName() + ": " + targetException.getMessage())
									.initCause(targetException);
						}
					}
				}, namingPattern);
			} else {
				registerScriptIterator(variable, (Args args) -> {
					try {
						return (Iterator<ScriptValue<?>>) method.invoke(null, args);

					} catch (IllegalAccessException e) {
						throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, args.context,
								"Access to function \"" + variable + "\" was refused");
					} catch (InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						if (targetException instanceof ScriptException) {
							throw (ScriptException) targetException;
						} else {
							throw (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, args.context,
									"Other error:\n" + targetException.getClass().getName() + ": " + targetException.getMessage())
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

	public ScriptValue<?> callAndRun(String variable) {
		return callAndRun(variable, 0, 0);
	}

	public ScriptValue<?> callAndRun(String variable, int lineNumber, int columnNumber)  {
		return this.callAndRun(variable, new Args(this), lineNumber, columnNumber);
	}

	public ScriptValue<?> callAndRun(String variable, Args args) {
		return callAndRun(variable, args, 0, 0);
	}

	public ScriptValue<?> callAndRun(String variable, Args args, int lineNumber, int columnNumber) {
		lastCall = new ContextStackTraceElement(this, variable, args,
				lineNumber, columnNumber);
		try {
			ScriptValue<?> value;

			ScriptsParser.StartContext funcStartContext;
			try {
				//noinspection ConstantConditions // for Intllij Idea saying the statement below could create a NullPointerException, but we are catching it anyway
				funcStartContext = Config.getCorrespondingContainingScripts(Type.FUNCTION, variable).parseTree;
				ScriptsExecutor visitor = new ScriptsExecutor(new Context(variable, Type.FUNCTION, this)); // baseVariables are not copied over.
				visitor.visit(funcStartContext);
				return visitor.getReturned();
			} catch (NullPointerException ignored) {
			}

			if (scriptFunctions.containsKey(variable)) {
				Pair<ScriptFunction, Args.NamingPattern> pair = scriptFunctions.get(variable);
				return pair.a.run(args.setNamingPattern(pair.b));
			}

			value = normalVariables.get(variable, this);
			if (value != null) {
				ScriptException.shouldHaveNoArgs(new ContextStackTraceElement(this,
						variable, args, lineNumber, columnNumber));
				return value;
			}

			value = globalVariables.get(variable, this);
			if (value != null) {
				ScriptException.shouldHaveNoArgs(new ContextStackTraceElement(this,
						variable, args, lineNumber, columnNumber));
				return value;
			}

			throw new ScriptException(ExceptionType.NOT_DEFINED, this, '\"' + variable + "\" does not exist.\"");
		}
		finally {
			lastCall = null;
		}
	}

	public static @Nullable Iterator<ScriptValue<?>> getIterator(String name, Args args) {
		if (scriptIterators.containsKey(name)) {
			Pair<ScriptIterator, Args.NamingPattern> pair = scriptIterators.get(name);
			return pair.a.run(args.setNamingPattern(pair.b));
		}
		return null;
	}
	 
	public void assign(String key, ScriptValue<?> value, boolean global, Token startToken) {
		assign(key, value, global, startToken.getLine(), startToken.getCharPositionInLine());
	}

	public void assign(String key, ScriptValue<?> value, boolean global, int lineNumber, int columnNumber) {
		if (scriptFunctions.containsKey(key)) {
			throw new ScriptException(ExceptionType.NOT_OVERRIDABLE,
					"You tried to create/modify \"" + key + "\", but it is already a default function.",
					new ContextStackTraceElement(this, "ASSIGNMENT TO " + key + " = " + value, lineNumber, columnNumber));
		} if (global && normalVariables.containsKey(key, this)) {
			throw new ScriptException(ExceptionType.NOT_OVERRIDABLE,
					"You tried to create/modify \"" + key + "\", but that name is already taken by a global variable.",
					new ContextStackTraceElement(this, "ASSIGNMENT TO " + key + " = " + value, lineNumber, columnNumber));
		} if (global || globalVariables.containsKey(key, this)) {
			globalVariables.put(key, (ScriptValue<Object>) value, this);
		} else {
			normalVariables.put(key, (ScriptValue<Object>) value, this);
		}
	}

	public void delete(String key) {
		normalVariables.remove(key, this);
		globalVariables.remove(key, this);
	}

	public static ScriptValue<?> trigger(Type scriptType, String scriptName, @Nullable StringScriptValueMap<Object> baseVariables, @Nullable Player triggeringPlayer) {
		CustomLogger.finer("running " + scriptType.name() + " " + scriptName + "(" + Types.getPrettyArgs(null, baseVariables) + ")");
		ScriptsParser.StartContext parseTree = Objects.requireNonNull(
				Config.getCorrespondingContainingScripts(scriptType, scriptName), scriptName + " doesn't exist as " + scriptType.name())
				.parseTree;
		ScriptsExecutor visitor = new ScriptsExecutor(new Context(scriptName, scriptType, baseVariables, triggeringPlayer));
		visitor.visit(parseTree);
		return visitor.getReturned();
	}

	public static ScriptThread threadTrigger(Type scriptType, String scriptName, @Nullable StringScriptValueMap<Object> baseVariables) {
		return threadTrigger(scriptType, scriptName, baseVariables, null);
	}

	public static ScriptThread threadTrigger(Type scriptType, String scriptName, @Nullable StringScriptValueMap<Object> baseVariables, @Nullable Player triggeringPlayer) {
		CustomLogger.finer("running " + scriptType.name() + " " + scriptName + "(" + Types.getPrettyArgs(null, baseVariables) + ") ASYNCHRONOUSLY");
		ScriptsParser.StartContext parseTree = Objects.requireNonNull(
				Config.getCorrespondingContainingScripts(scriptType, scriptName), scriptName + " doesn't exist as " + scriptType.name())
				.parseTree;
		ScriptThread scriptThread = new ScriptThread(parseTree, new Context(scriptName, scriptType, baseVariables, triggeringPlayer));
		scriptThread.start();
		return scriptThread;
	}

	private static final WeakHashMap<Integer, ScriptsParser.ExpressionContext> evalCache = new WeakHashMap<>();

	public ScriptValue<?> eval(String expression) {
		ScriptsParser.ExpressionContext parseTree = evalCache.computeIfAbsent(expression.hashCode(), hash -> {
			try {
				return Parsing.parseExpression("<eval(...)>", expression);
			} catch (IOException e) {
				throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, this, "Error with file encoding, Input Output or idk what went wrong");
			} catch (ScriptsParsingException e) {
				throw new ScriptException(ExceptionType.PARSING_ERROR, this, e.getMessage());
			}
		});
		ScriptsExecutor visitor = new ScriptsExecutor(new Context(scriptName, scriptType, normalVariables.clone(), triggeringPlayer));
		visitor.visit(parseTree);
		return visitor.getReturned();
	}

	private static final WeakHashMap<Integer, ScriptsParser.StartContext> execCache = new WeakHashMap<>();

	public ScriptValue<?> exec(String code) {
		ScriptsParser.StartContext parseTree = execCache.computeIfAbsent(code.hashCode(), hash -> {
			try {
				return Parsing.parse("<exec(...)>", code);
			} catch (IOException e) {
				throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, this, "Error with file encoding, Input Output or idk what went wrong");
			} catch (ScriptsParsingException e) {
				throw new ScriptException(ExceptionType.PARSING_ERROR, this, e.getMessage());
			}
		});
		ScriptsExecutor visitor = new ScriptsExecutor(new Context(scriptName, scriptType, normalVariables.clone(), triggeringPlayer));
		visitor.visit(parseTree);
		return visitor.getReturned();
	}

	@Override
	public Context clone() {
		try {
			Context clone = (Context) super.clone(); // it should work...
			clone.setParent(this);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw ScriptException.wrapInShouldNotHappen(e, this);
		}
	}

	public @Nullable Context getParent() {
		return parent;
	}

	public void setParent(@Nullable Context parent) {
		this.parent = parent;
	}

	/**
	 * @param currentLevel you can specify the last thing called that caused the error here, but
	 * @return the call hierarchy that led to the creation of this {@link Context}, where the first element is
	 * "currentLevel" if not null else {@link #lastCall}.
	 */
	public ScriptStackTraceElement[] getStackTrace(@Nullable ScriptStackTraceElement currentLevel) {
		ArrayList<ScriptStackTraceElement> stack = new ArrayList<>();
		if (currentLevel != null) stack.add(currentLevel);
		else if (lastCall != null) stack.add(lastCall);
		else stack.add(ScriptStackTraceElement.UNKNOWN);
		Context ctx = this;
		for (;;) {
			if (ctx.parent != null) stack.add(ctx.parent.lastCall != null ? ctx.parent.lastCall : ScriptStackTraceElement.UNKNOWN);
			else break;
		}
		return stack.toArray(new ScriptStackTraceElement[0]);
	}

	/**
	 * @return the player that triggered this command or event
	 * @throws ScriptException if {@link #triggeringPlayer} is null
	 */
	public @NotNull Player getTriggeringPlayer() {
		if (triggeringPlayer != null) return triggeringPlayer;
		throw new ScriptException(ExceptionType.INVALID_ARGUMENTS, this, "You didn't provide any player to the function, " +
				"and the function was executed in such a context that there is no player to execute the function with.");
	}
}