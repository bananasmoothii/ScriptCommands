package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configs_storage.Config;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.configs_storage.ContainingScripts;
import fr.bananasmoothii.scriptcommands.core.configs_storage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ExceptionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class Context implements Cloneable {
	
	protected final UsableFunctions usableFunctions;
	
	/**
	 * Calls {@link ScriptValueList#toScriptValueArrayList(Object[])} on the args.
	 * @param args should contain only strings
	 * @param baseVariables variables that will be given to the script, null for no other variable
	 */
	public Context(Object[] args, @Nullable HashMap<String, ScriptValue<?>> baseVariables) {
		this(ScriptValueList.toScriptValueArrayList(args), baseVariables);
	}

	/**
	 * @param args should contain only strings
	 * @param baseVariables variables that will be given to the script, null for no other variable
	 */
	public Context(ScriptValueList<?> args, @Nullable HashMap<String, ScriptValue<?>> baseVariables) {
		this.usableFunctions = new UsableFunctions();
		this.usableFunctions.volatileVariables.putAll(baseVariables);
		this.usableFunctions.volatileVariables.put("args", new ScriptValue<>(args));
	}

	/**
	 * Just make a new {@link Context} with the same {@link UsableFunctions}
	 */
	public Context(UsableFunctions usableFunctions) {
		this.usableFunctions = usableFunctions;
	}

	public ScriptValue<?> callAndRun(String variable) {return this.callAndRun(variable, new ScriptValueList<>());}
	public ScriptValue<?> callAndRun(String variable, ScriptValueList<?> args) {
		ScriptException exception = null;
		ScriptValue<?> value = null;

		ScriptsParser.StartContext funcStartContext;
		try {
			funcStartContext = Config.firstInstance.getCorrespondingContainingScripts(ContainingScripts.Type.FUNCTION, variable).parseTree;
			ScriptsExecutor visitor = new ScriptsExecutor(new Context(args, null)); // baseVariables are not copied over.
			visitor.visit(funcStartContext);
			return visitor.getReturned();
		}
		catch (NullPointerException ignored) { }

		try {
			Method method = usableFunctions.getClass().getDeclaredMethod(variable, ScriptValueList.class);
			return (ScriptValue<?>) method.invoke(usableFunctions, args);
		} catch (NoSuchMethodException e) {
			exception = new ScriptException(ExceptionType.NOT_DEFINED, variable, args,
					"function \"" + variable + "\" does not exists");
		} catch (IllegalAccessException e) {
			exception = new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
					"Access to function \"" + variable + "\" was refused");
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			if (targetException instanceof ScriptException) {
				exception = (ScriptException) targetException;
			} else {
				StringBuilder stackTrace = new StringBuilder();
				for (StackTraceElement element: targetException.getStackTrace()) {
					stackTrace.append(element.toString())
							.append('\n');
				}
				exception = new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, variable, args,
						"other error:\n" + targetException.getClass().getName() + ": " + targetException.getMessage() +
						"\n" + stackTrace.toString());
			}
		}

		value = this.usableFunctions.volatileVariables.get(variable);
		if (value != null) {
			ScriptException.shouldHaveNoArgs(variable, args);
			return value;
		}

		value = UsableFunctions.globalVariables.get(variable);
		if (value != null) {
			ScriptException.shouldHaveNoArgs(variable, args);
			return value;
		}

		throw exception;
	}
	 
	public void assign(String key, ScriptValue value, boolean global) {
		if (Arrays.stream(this.usableFunctions.getClass().getDeclaredMethods()).anyMatch(x -> key.equals(x.getName()))
				|| (global && this.usableFunctions.volatileVariables.get(key) != null)) {
			throw new ScriptException(ExceptionType.NOT_OVERRIDABLE, "<assignment to " + key + '>', "",
					"you tried to create/modify \"" + key + "\", but it is already a default function, or not a global" +
							" variable and you tried to make it global, that cannot be overridden.");
		}
		if (global || UsableFunctions.globalVariables.get(key) != null) {
			UsableFunctions.globalVariables.put(key, value);
		}
		else {
			this.usableFunctions.volatileVariables.put(key, value);
		}
	}
	//public void assign(String key, Object value, boolean global) {this.assign(key, new ScriptValue(value), global);}

	public void delete(String key) {
		this.usableFunctions.volatileVariables.remove(key);
		UsableFunctions.globalVariables.remove(key);
	}

	public Context clone() {
		UsableFunctions clonedUsableFunctions = new UsableFunctions();
		clonedUsableFunctions.volatileVariables = (HashMap<String, ScriptValue<?>>) usableFunctions.volatileVariables.clone();
		return new Context(clonedUsableFunctions);
	}

}