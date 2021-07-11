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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.SQLException;

/**
 * All exceptions from scripts will be this one with a name and optionally an error message.
 * @see ScriptException#invalidType making a basic invalid type faster
 */
public class ScriptException extends RuntimeException {

    /**
	 * for Better managing of exception types in the Scripts.
	 * @see ScriptException
	 */
	public enum ExceptionType {
		ASSERTION_ERROR("ASSERTION_ERROR"),
		CONVERSION_ERROR("CONVERSION_ERROR"),
		GLOBAL_NOT_ALLOWED("GLOBAL_NOT_ALLOWED"),
		UNAVAILABLE_THREAD_NAME("UNAVAILABLE_THREAD_NAME"),
		INVALID_ARGUMENTS("INVALID_ARGUMENTS"),
		INVALID_OPERATOR("INVALID_OPERATOR"),
		INVALID_TYPE("INVALID_TYPE"),
		NOT_A_CONTAINER("NOT_A_CONTAINER"),
		NOT_DEFINED("NOT_DEFINED"),
		NOT_OVERRIDABLE("NOT_OVERRIDABLE"),
		/** For example when you do {@code 10[1]}, it doesn't mean anything */
		NOT_LISTABLE("NOT_LISTABLE"),
		OUT_OF_BOUNDS("OUT_OF_BOUNDS"),
		/** For errors that should not happen */
		SHOULD_NOT_HAPPEN("SHOULD_NOT_HAPPEN"),
		UNKNOWN("UNKNOWN");

		private final String type;

		ExceptionType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return type;
		}
	}

	private final String type;
	private String function;
	private String funcArgs;

	/*
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but with
	 * <i>funcArgs</i> set to an empty string and
	 * <i>message</i> set to {@code "<no further information>"}<br/>
	 * Warning: providing no message is not recommended
	 * @see ScriptException#getMessage()
	 *
	public ScriptException(ExceptionType type, String function) {
		this(type, function, null);
	}

	/**
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but with
	 * <i>message</i> set to {@code null}<br/>
	 * Warning: providing no message is not recommended
	 * @see ScriptException#getMessage()
	 *
	public ScriptException(ExceptionType type, String function, @Nullable String funcArgs) {
		this(type, function, funcArgs, null);
	}

	 */

	/**
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but calls
	 * {@link Types#getPrettyArgs(ScriptValueList)} on <strong>funcArgs</strong>
	 */
	public ScriptException(ExceptionType type, String function, @NotNull ScriptValueList<?> funcArgs, String message) {
		this(type, function, Types.getPrettyArgs(funcArgs), message);
	}


	public ScriptException(ExceptionType exceptionType, Args args, String message) {
		this(exceptionType, StackTraceUtils.getFromStackTrace(0), args, message);
	}

	public ScriptException(ExceptionType exceptionType, String function, Args args, String message) {
		this(exceptionType, function, args.toString(), message);
	}

	/**
	 * Same as {@link ScriptException#ScriptException(String, String, String, String)} but calls
	 * {@link ExceptionType#toString()} on <strong>type</strong>
	 */
	public ScriptException(ExceptionType type, String function, @Nullable String funcArgs, String message) {
		this(type.toString(), function, funcArgs, message);
	}

	/**
	 * Constructs a new instance with every arguments<br/>
	 * Warning: providing no message is not recommended
	 * @see ScriptException#getMessage()
	 */
	public ScriptException(String type, String function, @Nullable String funcArgs, String message) {
		super(message);
		this.type = type;
		this.function = function;
		this.funcArgs = funcArgs == null ? "" : funcArgs;
	}

	/**
	 * @return {@code "Error " + type.toString() + " in " + function + "(" + funcArgs + "): " + super.getMessage()}
	 */
	@Override
	public String getMessage() {
		return "Script error \"" + type + "\" in " + function + "(" + funcArgs + ")" +
				(super.getMessage() == null ? "" : ": " + super.getMessage());
	}

	// ---- getters and setters ----

	public String getType() {
		return type;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getFuncArgs() {
		return funcArgs;
	}

	public void setFuncArgs(String funcArgs) {
		this.funcArgs = funcArgs;
	}

	public void appendToMessage(String messageToAppend) {
		appendToThrowableMessage(this, messageToAppend);
	}

	public static void appendToSQLExceptionQuery(SQLException sqlException, String query) {
		appendToThrowableMessage(sqlException, " (SQL query: \"" + query + "\")");
	}

	/**
	 * Uses reflection to append something to the detail message of an exception.
	 */
	public static void appendToThrowableMessage(Throwable throwable, String s) {
		try {
			Field field = Throwable.class.getDeclaredField("detailMessage");
			field.setAccessible(true);
			field.set(throwable, field.get(throwable) + s);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	// ---- for making ScriptsExceptions faster ----

	/**
	 * same as {@link ScriptException#invalidType(String, String, String, String)} except first argument is {@link StackTraceUtils#getFromStackTrace(int)}
	 * @see StackTraceUtils#getFromStackTrace(int)
	 */
	public static ScriptException invalidType(int depth, String excepted, String got, String gotTypes) {
		return invalidType(StackTraceUtils.getFromStackTrace(depth), excepted, got, gotTypes);
	}

	/**
	 * A faster way to declare a type as invalid
	 * @param function the function of the error
	 * @param excepted String of type excepted
	 * @param got ArrayList&lt;{@link ScriptValue}&gt; the plain arguments
	 * @return a new {@link ScriptValue} of type "INVALID_TYPE" with message <pre>{@code String.format("in %s(%s): excepted %s but got %s", function, Types.getArgsAsList(got), excepted, Types.getTypesAsList(got))}</pre>
	 * @see ScriptException#invalidType(String, String, String, String) function used as base
	 */
	public static ScriptException invalidType(String function, String excepted, ScriptValueList<?> got) {
		return invalidType(function, excepted, Types.getPrettyArgs(got), Types.getPrettyTypes(got));
	}

	/**
	 * This is the function used as base
	 * @return a new {@link ScriptValue} of type "INVALID_TYPE" with message <pre>{@code String.format("excepted %s but got %s", excepted, gotTypes)}</pre>
	 * @see ScriptException#invalidType(String, String, ScriptValueList)  function used as base
	 */
	public static ScriptException invalidType(String function, String excepted, String got, String gotTypes) {
		return new ScriptException(ExceptionType.INVALID_TYPE, function, got,
				String.format("excepted %s but got %s", excepted, gotTypes));
	}

	public static ScriptException invalidType(String function, String expected, String got, ScriptValue.SVType gotType) {
		return invalidType(function, expected, got, gotType.name);
	}
	
	public static ScriptException invalidOperator(String where, ScriptValue<?> left, String operator, ScriptValue<?> right) {
		return new ScriptException(ExceptionType.INVALID_OPERATOR, where, "", "invalid operator '" + operator + "' between " + left.type + " and " + right.type +
		(left.is(ScriptValue.SVType.DICTIONARY) && operator.equals("+") && right.is(ScriptValue.SVType.LIST) ? " because you can only add a list of two elements to a dictionary." :
		(left.is(ScriptValue.SVType.LIST) && operator.equals("-") && ! right.is(ScriptValue.SVType.INTEGER) ? " because the left value must be an Integer." : ".")));
	}

	/*
	public static void checkArgsNumber(int minArgs, ScriptValueList<?> args) {
		if (args.size() < minArgs)
			throw new ScriptException(ExceptionType.INVALID_ARGUMENTS,
					StackTraceUtils.getFromStackTrace(0),
					Types.getPrettyArgs(args),
					"too few arguments, excepted at least " + minArgs + " but got " + args.size());
	}

	public static void checkArgsNumber(int minArgs, int maxArgs, Args args) {
		checkArgsNumber(minArgs, maxArgs, args, StackTraceUtils.getFromStackTrace(0));
	}

	public static void checkArgsNumber(int minArgs, int maxArgs, Args args, String funcName) {
		if (args.getArgsList().size() < minArgs || args.getArgsList().size() > maxArgs)
			throw new ScriptException(ExceptionType.INVALID_ARGUMENTS,
					funcName,
					Types.getPrettyArgs(args),
					"too few arguments, excepted from " + minArgs + " to " + maxArgs + " but got " + args.size());
	}
	*/
	
	public static void shouldHaveNoArgs(String variable, Args args) {
		if (! args.isEmpty()) {
			CustomLogger.warning("You called the variable " + variable + " with arguments (" + args.toString() + ") but you shouldn't.\n" +
			                   "doing as if there where no args");
		}
	}

	/**
	 * @see #toScriptException(Throwable, int, String)
	 */
	public static ScriptException toScriptException(Throwable exception) {
		return toScriptException(exception, 1);
	}

	/**
	 * @see #toScriptException(Throwable, int, String)
	 */
	public static ScriptException toScriptException(Throwable exception, int depth) {
		return toScriptException(exception, depth, null);
	}

	/**
	 * Converts any {@link Throwable} in {@link ScriptException} as {@linkplain ExceptionType#SHOULD_NOT_HAPPEN SHOULD_NOT_HAPPEN}
	 * @param exception the base exception
	 * @param depth to know what function is problematic
	 * @param appendix (nullable) for directly adding something to the error message
	 * @return a new constructed {@link ScriptException}
	 */
	public static ScriptException toScriptException(Throwable exception, int depth, @Nullable String appendix) {
		ScriptException e = new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, StackTraceUtils.getFromStackTrace(depth), "",
				"Inner SQL error: " + exception.getMessage() + (appendix == null ? "" : appendix));
		e.setStackTrace(exception.getStackTrace());
		return e;

		/*StringBuilder errorTrace = new StringBuilder();
		errorTrace.append('\n');
		for (StackTraceElement stackTraceElement: exception.getStackTrace()) {
			errorTrace.append(stackTraceElement.toString())
					.append('\n');                                                     // what's that shit ??????
		}
		return (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, StackTraceUtils.getFromStackTrace(depth), "",
				"Inner SQL error: " + exception.getMessage() +
				"\nReal stack trace is between these line:\n-----------------------------------\n" +
				exception.getClass().getName() + ": " + exception.getMessage() + "\n" + errorTrace +
				"-----------------------------------\n\"wrong\" stack trace (you don't need to copy it):").initCause(exception);

		 */
	}

	/**
	 * @see ScriptException#toScriptException(SQLException, int, String)
	 */
	public static ScriptException toScriptException(SQLException exception, String query) {
		return toScriptException(exception, 1, query);
	}

	/**
	 * Similar to {@link ScriptException#toScriptException(Throwable, int, String)} but with {@code " (SQL query: " + query + ")"} as appendix
	 */
	public static ScriptException toScriptException(SQLException exception, int depth, String query) {
		return toScriptException((Throwable) exception, depth + 1, " (SQL query: \"" + query + "\")");
	}


	public static <O> O requireNonNullElseThrow(O obj, RuntimeException runtimeException) {
		if (obj == null) throw runtimeException;
		return obj;
	}
	
}