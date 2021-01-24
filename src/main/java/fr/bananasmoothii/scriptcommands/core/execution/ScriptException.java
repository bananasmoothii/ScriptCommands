package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		INDISPONIBLE_THREAD_NAME("INDISPONIBLE_THREAD_NAME"),
		INVALID_ARG_NUMBER("INVALID_ARG_NUMBER"),
		INVALID_OPERATOR("INVALID_OPERATOR"),
		INVALID_TYPE("INVALID_TYPE"),
		NOT_A_CONTAINER("NOT_A_CONTAINER"),
		NOT_DEFINED("NOT_DEFINED"),
		NOT_OVERRIDABLE("NOT_OVERRIDABLE"),
		NOT_SUBSCRIPTABLE("NOT_SUBSCRIPTABLE"),
		OUT_OF_BOUNDS("OUT_OF_BOUNDS"),
		/** For errors that should not happen */
		SHOULD_NOT_HAPPEN("SHOULD_NOT_HAPPEN"),
		UNKNOWN("UNKNOWN"),
		;

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

	/**
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but with
	 * <i>funcArgs</i> set to an empty string and
	 * <i>message</i> set to {@code "<no further information>"}<br/>
	 * Warning: providing no message is not recommended
	 * @see ScriptException#getMessage()
	 */
	public ScriptException(ExceptionType type, String function) {
		this(type, function, null);
	}

	/**
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but with
	 * <i>message</i> set to {@code null}<br/>
	 * Warning: providing no message is not recommended
	 * @see ScriptException#getMessage()
	 */
	public ScriptException(ExceptionType type, String function, @Nullable String funcArgs) {
		this(type, function, funcArgs, null);
	}

	/**
	 * Same as {@link ScriptException#ScriptException(ExceptionType, String, String, String)} but calls
	 * {@link Types#getPrettyArgs(ScriptValueList)} on <strong>funcArgs</strong>
	 */
	public ScriptException(ExceptionType type, String function, @NotNull ScriptValueList<?> funcArgs,
						   /*@Nullable*/ String message) {
		this(type, function, Types.getPrettyArgs(funcArgs), message);
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
		return "Error \"" + type.toString() + "\" in " + function + "(" + funcArgs + ")" +
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
	
	public static ScriptException invalidOperator(String where, ScriptValue<?> left, String operator, ScriptValue<?> rigth) {
		return new ScriptException(ExceptionType.INVALID_OPERATOR, where, "", "invalid operator '" + operator + "' between " + left.type + " and " + rigth.type +
		(left.is("Dictionary") && operator.equals("+") && rigth.is("List") ? " because you can only add a list of two elements to a dictionary." :
		(left.is("List") && operator.equals("-") && ! rigth.is("Integer") ? " because the left value must be an Integer." : ".")));
	}


	public static void checkArgsNumber(int minArgs, ScriptValueList<?> args) {
		if (args.size() < minArgs)
			throw new ScriptException(ExceptionType.INVALID_ARG_NUMBER,
					StackTraceUtils.getFromStackTrace(0),
					Types.getPrettyArgs(args),
					"too few arguments, excepted at least " + minArgs + " but got " + args.size());
	}

	public static void checkArgsNumber(int minArgs, int maxArgs, ScriptValueList<?> args) {
		checkArgsNumber(minArgs, maxArgs, args, 0);
	}

	public static void checkArgsNumber(int minArgs, int maxArgs, ScriptValueList<?> args, int depth) {
		if (args.size() < minArgs || args.size() > maxArgs)
			throw new ScriptException(ExceptionType.INVALID_ARG_NUMBER,
					StackTraceUtils.getFromStackTrace(depth),
					Types.getPrettyArgs(args),
					"too few arguments, excepted from " + minArgs + " to " + maxArgs + " but got " + args.size());
	}
	
	public static void shouldHaveNoArgs(String variable, ScriptValueList<?> args) {
		if (args.size() > 0) {
			System.out.println("WARNING: You called the variable " + variable + " with arguments (" + Types.getPrettyArgs(args) + ") but you shouldn't.\n" +
			                   "doing as if there where no args");
		}
	}
	
}