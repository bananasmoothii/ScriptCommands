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
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParserBaseVisitor;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ExceptionType;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;

import java.util.*;

@SuppressWarnings("unchecked")
public class ScriptsExecutor extends ScriptsParserBaseVisitor<ScriptValue<?>> { // should only implement ScriptsVisitor
	
	private static final ScriptValue<NoneType> NONE = new ScriptValue<>(null);
	
	protected final Context context;

	private ScriptValue<?> returned = NONE;
	/** Can be:
	 * <ul><li>{@code 'n'} for nothing</li>
	 * <li>{@code 'b'} for simple boucle break</li>
	 * <li>{@code 'c'} for boucle continue</li>
	 * <li>{@code 'r'} for return, breaking everything</li></ul>
	 */
	private char breaking = 'n';
	
	public ScriptsExecutor(Context context) {
		this.context = context;
	}
	
	public ScriptValue<?> getReturned() {
		return returned;
	}

	public void forceReturn() {
		this.breaking = 'r';
	}
	
	@Override
	public ScriptValue<?> visit(ParseTree tree) {
		if (breaking != 'n') return null;
		return super.visit(tree);
	}
	
	/**
	 * Start for parsing. It catches any {@link ScriptException} and prints them.
	 */
	@Override
	public ScriptValue<NoneType> visitStart(ScriptsParser.StartContext ctx) {
		try {
			visitChildren(ctx);
		} catch (ScriptException e) {
			CustomLogger.severe(e.getMessage());
			if (CustomLogger.getLevel().intValue() <= 300) {
				e.printStackTrace();
			}
		}
		return NONE;
	}
	
	@Override
	public ScriptValue<NoneType> visitIf_block(ScriptsParser.If_blockContext ctx) {
		int i;
		for (i = 0; i < ctx.comparison().size(); i++) {
			if (determineBoolean(visitComparison(ctx.comparison(i)))) {
				visit(ctx.block(i));
				return NONE;
			}
		}
		if (ctx.ELSE() != null) {
			visit(ctx.block(i));
		}
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitSwitch_block(ScriptsParser.Switch_blockContext ctx) {
		ScriptValue<?> mainExpression = visitExpression(ctx.expression());
		for (ScriptsParser.Case_blockContext case_blockContext: ctx.case_block()) {
			for (ScriptsParser.ExpressionContext expressionContext: case_blockContext.expression()) {
				if (mainExpression.equals(visitExpression(expressionContext))) {
					visit(case_blockContext.block());
					return NONE;
				}
			}
		}
		if (ctx.ELSE() != null) {
			visit(ctx.block());
		}
		return NONE;
	}

	/**
	 * A class just for the function {@link ScriptsExecutor#visitFor_block(ScriptsParser.For_blockContext)}
	 */
	public static class ScriptValueIntegerIterator implements Iterator<ScriptValue<Integer>> {

		private int start; // start is used as current value
		private final int stop, step;

		public ScriptValueIntegerIterator(int start, int stop, int step) {
			this.start = start;
			this.stop = stop;
			this.step = step;
		}

		/*
		public ScriptValueIntegerIterator(ScriptValueList<?> args) {
			for (ScriptValue<?> arg: args) {
				if (!arg.isNumber()) throw ScriptException.invalidType("range", "Decimals or Integers", args);
			}
			start = 0;
			step = 1;
			if (args.size() == 1) stop = args.get(0).asInteger();
			else {
				start = args.get(0).asInteger();
				stop = args.get(1).asInteger();
				if (args.size() == 3) step = args.get(2).asInteger();
			}
		}
		*/

		@Override
		public boolean hasNext() {
			return start < stop;
		}

		@Override
		public ScriptValue<Integer> next() {
			int ret = start;
			start += step;
			return new ScriptValue<>(ret);
		}
	}

	/**
	 * A class just for the function {@link ScriptsExecutor#visitFor_block(ScriptsParser.For_blockContext)}
	 */
	public static class ScriptValueDoubleIterator implements Iterator<ScriptValue<Double>> {

		private double start;
		private final double stop, step;

		public ScriptValueDoubleIterator(double start, double stop, double step) {
			this.start = start;
			this.stop = stop;
			this.step = step;
		}

		@Override
		public boolean hasNext() {
			return start < stop;
		}

		@Override
		public ScriptValue<Double> next() {
			double ret = start;
			start += step;
			return new ScriptValue<>(ret);
		}
	}

	private static final Args.NamingPattern rangeNP = new Args.NamingPattern()
			.setNamingPattern(1, "stop")
			.setNamingPattern("start", "stop", "step")
			.setDefaultValue("start", 0)
			.setDefaultValue("step", 1);

	@Override
	public ScriptValue<NoneType> visitFor_block(ScriptsParser.For_blockContext ctx) {
		Iterator<ScriptValue<?>> iterator;
		if (ctx.expression().expression_part().function().size() == 1 &&
				ctx.expression().expression_part().function(0).get_from_list().size() == 0 &&
				ctx.expression().expression_part().function(0).VARIABLE().getText().equals("range")) {
			Args args = visitArgs(ctx.expression().expression_part().function(0).arguments())
					.setNamingPattern(rangeNP);
			//ScriptException.checkArgsNumber(1, 3, args, "range");  // making a custom iterator for not having to first create the hole list. This can be considered as cheating lol
			ScriptValue<?> start = args.getArg("start"),
					       stop  = args.getArg("stop"),
					       step  = args.getArg("step");
			boolean allInt = start.is("Integer") && stop.is("Integer") && step.is("Integer");
			if (allInt) //noinspection rawtypes
				iterator = (Iterator<ScriptValue<?>>) (Iterator) new ScriptValueIntegerIterator(start.asInteger(), stop.asInteger(), step.asInteger()); // messy but best way //TODO: test
			else //noinspection rawtypes
				iterator = (Iterator<ScriptValue<?>>) (Iterator) new ScriptValueDoubleIterator(start.asDouble(), stop.asDouble(), step.asDouble());
		} else {
			ScriptValue<?> exprToIterate = visitExpression(ctx.expression());
			iterator = exprToIterate.iterator(ctx.FOR().getText() + " " + ctx.VARIABLE().getText() +
					" " + ctx.IN().getText() + " " + ctx.expression().getText() + " {...}");
		}
		while (iterator.hasNext()) {
			if (breaking == 'c')
				breaking = 'n';
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false);
			visit(ctx.block());
			if (breaking == 'b') {
				breaking = 'n';
				break;
			}
			if(breaking == 'r')
				break;
		}
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitWhile_block(ScriptsParser.While_blockContext ctx) {
		while (breaking != 'b' && breaking != 'r' && determineBoolean(visitExpression(ctx.expression()))) {
			if (breaking == 'c')
				breaking = 'n';
			visit(ctx.block());
		}
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitTry_block(ScriptsParser.Try_blockContext ctx) {
		try {
			visit(ctx.block());
		} catch (ScriptException e) {
			for (ScriptsParser.Catch_blockContext catchCtx: ctx.catch_block()) {
				List<ScriptsParser.ExpressionContext> expressions = catchCtx.expression();
				for (int i = 0; i < expressions.size(); i++) {
					ScriptValue<?> expr = visitExpression(expressions.get(i));
					if (! expr.is("Text") && ! expr.is("Boolean")) {
						ScriptValueList<Object> arr = new ScriptValueList<>();
						arr.add((ScriptValue<Object>) expr);
						throw ScriptException.invalidType("try {...} catch ", "Text or Boolean", arr);
					}
					if ( (expr.is("Text") && expr.asString().equals( e.getType() ))
							| (expr.is("Boolean") && expr.asBoolean())) {
						if (catchCtx.varToAssign.size() > i && catchCtx.varToAssign.get(i) != null)
							context.assign(catchCtx.varToAssign.get(i).getText(), expr, false);
						visit(catchCtx.block());
						return NONE;
					}
				}
			}
			throw e;
		}
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitBreak(ScriptsParser.BreakContext ctx) {
		breaking = 'b';
		return NONE;
	}
	
	@Override
	public ScriptValue<NoneType> visitContinue(ScriptsParser.ContinueContext ctx) {
		breaking = 'c';
		return NONE;
	}
	
	@Override
	public ScriptValue<NoneType> visitReturn(ScriptsParser.ReturnContext ctx) {
		if (ctx.expression() != null)
			returned = visit(ctx.expression());
		breaking = 'r';
		return NONE;
	}
	
	@Override
	public ScriptValue<?> visitExpression_part(ScriptsParser.Expression_partContext ctx) {
		ScriptValue<?> lastValue = visit(ctx.expression_between_dots()); // no "visitExpression_between_dots"
		for (int i = 0; i < ctx.function().size(); i++) {
			lastValue = visitFuncPlusArg(ctx.function(i), lastValue);
		}
		return lastValue;
	}

	@Override
	public ScriptValue<NoneType> visitThread_expression(ScriptsParser.Thread_expressionContext ctx) {
		boolean presentId = ctx.expression().size() == 2;
		ScriptValue<?> threadId = presentId ? visitExpression(ctx.expression(0)) : null;
		ScriptsParser.ExpressionContext ctxToExecute = presentId ? ctx.expression(1) : ctx.expression(0);
		try {
			new ScriptThread(context, ctxToExecute, threadId).start();
		} catch (InvalidNameException e) {
			if (threadId != null)
				throw new ScriptException(ExceptionType.UNAVAILABLE_THREAD_NAME, "thread " + ctx.expression(0).getText() + " ", threadId.toString(), "The name \"" + threadId.toString() + "\" is already taken by an other thread.");
			else
				throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, "thread " + ctx.expression(0).getText() + " ", "<no id provided>", "This is an error that should not happen when no ID is specified.");
		}
		return NONE;
	}

	@Override
	public ScriptValue<?> visitThread_block(ScriptsParser.Thread_blockContext ctx) {
		ScriptValue<?> threadId = ctx.expression() != null ? visitExpression(ctx.expression()) : null;
		ScriptsParser.BlockContext ctxToExecute = ctx.block();
		try {
			new ScriptThread(context, ctxToExecute, threadId).start();
		} catch (InvalidNameException e) {
			if (threadId != null)
				throw new ScriptException(ExceptionType.UNAVAILABLE_THREAD_NAME, "thread " + ctx.expression().getText() + " ", threadId.toString(), "The name \"" + threadId.toString() + "\" is already taken by an other thread.");
			else
				throw new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, "thread " + ctx.expression().getText() + " ", "<no id provided>", "This is an error that should not happen when no ID is specified.");
		}
		return NONE;
	}

	@Override
	public ScriptValue<?> visitThrow_(ScriptsParser.Throw_Context ctx) {
		throw new ScriptException(visitExpression(ctx.expression(0)).asString(), "throw ", ctx.expression(0).getText(), ctx.expression().size() == 2 ? visitExpression(ctx.expression(1)).asString() : "<no error message provided>");
	}

	@Override
	public ScriptValue<?> visitFunction(ScriptsParser.FunctionContext ctx) {
		return visitFuncPlusArg(ctx, null);
	}
	public ScriptValue<?> visitFuncPlusArg(ScriptsParser.FunctionContext ctx, @Nullable ScriptValue<?> arg) {
		Args args;
		if (ctx.arguments() != null)
			args = visitArgs(ctx.arguments());
		else
			args = new Args();
		if (arg != null)
			args.add(0, (ScriptValue<Object>) arg);

		ScriptValue<?> executed = context.callAndRun(ctx.VARIABLE().getText(), args);
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			executed = get_from_list(executed, getFromListContext, ctx.getText());
		}
		return executed;
	}

	public Args visitArgs(ScriptsParser.ArgumentsContext ctx) {
		Args args = new Args();
		for (ScriptsParser.ExpressionContext exprCtx: ctx.expression()) {
			args.add((ScriptValue<Object>) visitExpression(exprCtx));
		}
		for (ScriptsParser.SimpleAssignmentContext simpleAssignmentContext : ctx.simpleAssignment()) {
			args.add(simpleAssignmentContext.VARIABLE().getText(), (ScriptValue<Object>) visitExpression(simpleAssignmentContext.expression()));
		}
		for (ScriptsParser.SplatListContext splatCtx: ctx.splatList()) {
			ScriptValue<?> visited = visitSplatList(splatCtx);
			if ( ! visited.is("List"))
				throw ScriptException.invalidType("[splat] \"" + splatCtx.getText() + "\" ", "List", Types.getPrettyArg(visited), visited.type);
			args.getArgsList().addAll(visited.asList());
		}
		for (ScriptsParser.SplatDictContext splatCtx: ctx.splatDict()) {
			ScriptValue<?> visited = visitSplatDict(splatCtx);
			if ( ! visited.is("Dictionary"))
				throw ScriptException.invalidType("[splat] \"" + splatCtx.getText() + "\" ", "Dictionary", Types.getPrettyArg(visited), visited.type);
			StringScriptValueMap<Object> argsMap = args.getArgsMap();
			for (Map.Entry<ScriptValue<Object>, ScriptValue<Object>> entry: visited.asMap().entrySet()) {
				if (! entry.getKey().is("String"))
					throw ScriptException.invalidType("[splat] \"" + splatCtx.getText() + "\" ", "a Dictionary with only strings as keys", entry.getKey().toString(), entry.getKey().type);
				argsMap.put(entry.getKey().asString(), entry.getValue());
			}
		}
		return args;
	}

	/**
	 * Use {@link #visitArgs(ScriptsParser.ArgumentsContext)} instead.
	 */
	@Override
	@Contract("_ -> fail")
	@Deprecated
	public ScriptValue<ScriptValueList<?>> visitArguments(ScriptsParser.ArgumentsContext ctx) { // I have no better solution for this.
		throw new UnsupportedOperationException("this method should never be called");
	}
	
	public static ScriptValue<?> calculate(ScriptValue<?> a, Token operator, ScriptValue<?> b, String where, String varName) {return calculate(a, operator, b, where, varName, false);}
	public static ScriptValue<?> calculate(ScriptValue<?> a, Token operator, ScriptValue<?> b, String where, String varName, boolean global) {
		//System.out.println("calculate: " + a.v + operator.getText() + b.v);
		ScriptException error = ScriptException.invalidOperator(where, a, operator.getText(), b);
		switch (operator.getType()) {
			case ScriptsParser.PLUS:
				if (a.is("Integer") && b.is("Integer")) 
					return new ScriptValue<>(a.asInteger() + b.asInteger());
				else if (a.isNumber() && b.isNumber())
					return new ScriptValue<>(a.asDouble() + b.asDouble());
				else if ((a.is("Text") && (b.isNumber() || b.is("Text"))) || (b.is("Text") && a.isNumber()))
					return new ScriptValue<>(a.toString() + b.toString());
				else {
					if (global)
						throw new ScriptException(ExceptionType.GLOBAL_NOT_ALLOWED, where, "",
								"special function of operator '" + operator.getText() + "' between " + a.type + " and " +
								b.type + " cannot be global, that doesn't mean anything.");
					if (a.is("List")) {
						a.asList().add((ScriptValue<Object>) b);
						return NONE;
					}
					else if (a.is("Dictionary") && b.is("List")) {
						if (b.asList().size() != 2)
							throw error;
						a.asMap().put(b.asList().get(0), b.asList().get(1));
						return NONE;
					}
					else
						throw error;
				}
			case ScriptsParser.MINUS:
				if (a.is("Integer") && b.is("Integer"))
					return new ScriptValue<>(a.asInteger() - b.asInteger());
				else if (a.isNumber() && b.isNumber())
					return new ScriptValue<>(a.asDouble() - b.asDouble());
				else {
					if (global)
						throw new ScriptException(ExceptionType.GLOBAL_NOT_ALLOWED, where, "",
								"special function of operator '" + operator.getText() + "' between " + a.type + " and " +
								b.type + " cannot be global, that doesn't mean anything.");
					if (a.is("List")) {
						if (! b.is("Integer"))
							throw error;
						//noinspection SuspiciousMethodCalls
						a.asList().remove(b.asInteger());
						return NONE;
					}
					else if (a.is("Dictionary")) {
						if (! a.asMap().containsKey(b))
							throw new ScriptException(ExceptionType.NOT_DEFINED, where, varName + "[" + Types.getPrettyArg(b) + "]", "you tried to remove element " + Types.getPrettyArg(b) + " of Dictionary " + varName +
									" but it's not there. You can call \"" + varName + ".keyList\" to get a list of keys.");
						a.asMap().remove(b);
						return NONE;
					}
					else throw error;
				}
				//break;
			case ScriptsParser.TIMES:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is("Integer") && b.is("Integer"))
					return new ScriptValue<>(a.asInteger() * b.asInteger());
				else
					return new ScriptValue<>(a.asDouble() * b.asDouble());
				//break;
			case ScriptsParser.DIVIDE:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				return new ScriptValue<>(a.asDouble() / b.asDouble());
				//break;
			case ScriptsParser.FLOOR_DIVIDE:
				return new ScriptValue<>((int) (a.asDouble() / b.asDouble()));
				//break;
			case ScriptsParser.MODULO:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is("Integer") && b.is("Integer"))
					return new ScriptValue<>(a.asInteger() % b.asInteger());
				else
					return new ScriptValue<>(a.asDouble() % b.asDouble());
				//break;
			case ScriptsParser.POW:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is("Integer") && b.is("Integer"))
					return new ScriptValue<>((int) Math.round(Math.pow(a.asInteger(), b.asInteger())));
				else
					return new ScriptValue<>(Math.pow(a.asDouble(), b.asDouble()));
				//break;
			default:
				throw error;
		}
	}
	
	@Override
	public ScriptValue<NoneType> visitAssignment(ScriptsParser.AssignmentContext ctx) {
		String varName = ctx.VARIABLE().getText();
		ScriptValue<?> value = visitExpression(ctx.expression());
		boolean global = ctx.GLOBAL() != null;

		if (ctx.operator != null) {
			ScriptValue<?> varValue = context.callAndRun(varName);
			context.assign(varName, calculate(varValue, ctx.operator, value.clone(), ctx.getText(), varName, global), global);
		}
		else
			context.assign(varName, value.clone(), global);
		return NONE;
	}
	
	@Override
	public ScriptValue<NoneType> visitDeletion(ScriptsParser.DeletionContext ctx) {
		context.delete(ctx.VARIABLE().getText()); // easy :D
		return NONE;
	}
	
	protected ScriptValue<?> get_from_list(ScriptValue<?> obj, ScriptsParser.Get_from_listContext getFromListContext, String nameOfVar) {
		if (getFromListContext.nbSingle != null) {
			if (obj.is("List")) {
				int number = visitExpression(getFromListContext.nbSingle).asInteger();
				int size = obj.asList().size();
				if (number >= size || number <= -size) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", nameOfVar + "[" + number + "]", "you asked for element " + number + " of List " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-size + 1) + " to " + (size - 1));
				}
				if (number < 0) number += size;
				return obj.asList().get(number);
			}
			else if (obj.is("Dictionary")) {
				ScriptValue<?> key = visitExpression(getFromListContext.nbSingle);
				ScriptValue<?> element = obj.asMap().get(key);
				if (element == null) {
					throw new ScriptException(ExceptionType.NOT_DEFINED, "[get element from list] ", nameOfVar + "[" + Types.getPrettyArg(key) + "]", "you asked for element " + Types.getPrettyArg(key) + " of Dictionary " + nameOfVar +
							" but it does not exist. You can call \"" + nameOfVar + ".keyList\" to get a list of keys.");
				}
				return element;
			}
			else if (obj.is("Text")) {
				int number = visitExpression(getFromListContext.nbSingle).asInteger();
				int size = obj.asString().length();
				if (number >= size || number <= -size) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", nameOfVar + "[" + number + "]", "you asked for character " + number + " of List " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-size + 1) + " to " + (size - 1));
				}
				if (number < 0) {
					number += size;
				}
				return new ScriptValue<>(String.valueOf(obj.asString().charAt(number)));
			}
			else
				throw new ScriptException(ExceptionType.NOT_LISTABLE, "[get element from list] ", nameOfVar + '[' + getFromListContext.nbSingle.getText() + ']', nameOfVar +
						" isn't a List, a Dictionary or Text, so you can't ask for a specific element in it");
		}
		else {
			if (obj.is("List")) {
				Integer nb1 = getFromListContext.nb1 == null ? null : visitExpression(getFromListContext.nb1).asInteger();
				Integer nb2 = getFromListContext.nb2 == null ? null : visitExpression(getFromListContext.nb2).asInteger();
				if (nb1 == null && nb2 == null)
					return obj;
				int size = obj.asList().size();
				String funcArgs = nameOfVar + "[" + toEmptyStringIfNull(nb1) + ":" + toEmptyStringIfNull(nb2) + "]";
				if (nb1 != null && (nb1 >= size || nb1 <= -size)) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", funcArgs, "you asked for element " + nb1 + " of List " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-(size - 1)) + " to " + (size - 1));
				}
				if (nb2 != null && (nb2 >= size || nb2 <= -size)) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", funcArgs, "you asked for element " + nb2 + " of List " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-(size - 1)) + " to " + (size - 1));
				}
				if (nb1 != null) {
					if (nb1 < 0)
						nb1 += size;
						
					if (nb2 != null) {
						if (nb2 < 0)
							nb2 += size;
						return new ScriptValue<>(obj.asList().subList(nb1, nb2));
					}
					else {
						return new ScriptValue<>(obj.asList().subList(nb1, obj.asList().size()));
					}
				}
				else {
					if (nb2 < 0)
						nb2 += size;
					return new ScriptValue<>(obj.asList().subList(0, nb2));
				}
			}
			else if (obj.is("Text")) {
				Integer nb1 = visitExpression(getFromListContext.nb1).asInteger();
				Integer nb2 = visitExpression(getFromListContext.nb2).asInteger();
				if (nb1 == null && nb2 == null)
					return obj;
				int size = obj.asString().length();
				if (nb1 != null && (nb1 >= size || nb1 <= -size)) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", nameOfVar + "[" + nb1 + ":" + toEmptyStringIfNull(nb2) + "]", "you asked for character " + nb1 + " of Text " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-(size - 1)) + " to " + (size - 1));
				}
				if (nb2 != null && (nb2 >= size || nb2 <= -size)) {
					throw new ScriptException(ExceptionType.OUT_OF_BOUNDS, "[get element from list] ", nameOfVar + "[" + toEmptyStringIfNull(nb2) + ":" + nb2 + "]", "you asked for character " + nb2 + " of Text " + nameOfVar +
							" but it contains only " + size + " elements, so you can ask for element from " + (-(size - 1)) + " to " + (size - 1));
				}
				if (nb1 != null) {
					if (nb1 < 0)
						nb1 += size;
						
					if (nb2 != null) {
						if (nb2 < 0)
							nb2 += size;
						return new ScriptValue<>(obj.asString().substring(nb1, nb2));
					}
					else {
						return new ScriptValue<>(obj.asString().substring(nb1));
					}
				}
				else {
					if (nb2 < 0)
						nb2 += size;
					return new ScriptValue<>(obj.asString().substring(0, nb2));
				}
			}
			else {
				String nb1 = getFromListContext.nb1 == null ? "" : getFromListContext.nb1.getText();
				String nb2 = getFromListContext.nb2 == null ? "" : getFromListContext.nb2.getText();
				throw new ScriptException(ExceptionType.NOT_LISTABLE, "[get element from list] ", nameOfVar + '[' + nb1 + ':' + nb2 + ']', nameOfVar +
						" isn't a List, a Dictionary or Text, so you can't ask for a specific element in it");
			}
		}
	}
	
	@Override
	public ScriptValue<Double> visitExprDecimal(ScriptsParser.ExprDecimalContext ctx) {
		return new ScriptValue<>(Double.valueOf(ctx.DECIMAL().getText().replace("_", "")));
	}
	
	@Override
	public ScriptValue<Integer> visitExprInteger(ScriptsParser.ExprIntegerContext ctx) {
		return new ScriptValue<>(Integer.valueOf(ctx.INTEGER().getText().replace("_", "")));
	}

	@Override
	public ScriptValue<?> visitExprBoolean(ScriptsParser.ExprBooleanContext ctx) {
		return new ScriptValue<>(ctx.TRUE() != null);
	}

	@Override
	public ScriptValue<NoneType> visitExprNone(ScriptsParser.ExprNoneContext ctx) {
		return NONE;
	}
	
	@Override
	public ScriptValue<String> visitExprText(ScriptsParser.ExprTextContext ctx) {
		String textText = ctx.TEXT().getText();
		ScriptValue<String> realText = new ScriptValue<>(textText.substring(1, ctx.TEXT().getText().length() -1).replace("\"\"", "\""));
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			realText = (ScriptValue<String>) get_from_list(realText, getFromListContext, textText);
		}
		return realText;
	}
	
	@Override
	public ScriptValue<String> visitExprFText(ScriptsParser.ExprFTextContext ctx) {
		ScriptValue<String> realText = visitFormatted_text(ctx.formatted_text());
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			realText = (ScriptValue<String>) get_from_list(realText, getFromListContext, ctx.formatted_text().getText());
		}
		return realText;
	}
	
	@Override
	public ScriptValue<?> visitExprParExpr(ScriptsParser.ExprParExprContext ctx) {
		ScriptValue<?> expr = visitExpression(ctx.expression());
		for (ScriptsParser.Get_from_listContext getFromListContext : ctx.get_from_list()) {
			expr = get_from_list(expr, getFromListContext, ctx.expression().getText());
		}
		return expr;
	}
	
	@Override
	public ScriptValue<ScriptValueList<Object>> visitExprList(ScriptsParser.ExprListContext ctx) {
		ScriptValue<ScriptValueList<Object>> list = visitList(ctx.list());
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			list = (ScriptValue<ScriptValueList<Object>>) get_from_list(list, getFromListContext, ctx.list().getText());
		}
		return list;
	}
	
	@Override
	public ScriptValue<ScriptValueMap<Object, Object>> visitExprDictionary(ScriptsParser.ExprDictionaryContext ctx) {
		ScriptValue<ScriptValueMap<Object, Object>> dict = visitDictionary(ctx.dictionary());
		for (ScriptsParser.Get_from_listContext getFromListContext : ctx.get_from_list()) {
			dict = (ScriptValue<ScriptValueMap<Object, Object>>) get_from_list(dict, getFromListContext, ctx.dictionary().getText());
		}
		return dict;
	}
	
	@Override
	public ScriptValue<ScriptValueList<Object>> visitList(ScriptsParser.ListContext ctx) {
		if (ctx.FOR() == null) {
			ScriptValueList<Object> list = new ScriptValueList<>();
			for (ScriptsParser.ExpressionContext elemCtx: ctx.expression()) {
				list.add((ScriptValue<Object>) visitExpression(elemCtx));
			}
			return new ScriptValue<>(list);
		}
		ScriptValueList<Object> list = new ScriptValueList<>();
		ScriptValue<?> exprToIterate = visitExpression(ctx.expression(1));
		ScriptsParser.ComparisonContext ifStatement = ctx.comparison();
		Iterator<ScriptValue<?>> iterator = exprToIterate.iterator(ctx.getText());
		while (iterator.hasNext()) {
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false);
			if (determineBoolean(visitComparison(ifStatement)))
				list.add((ScriptValue<Object>) visitExpression(ctx.expression(0)));
		}
		return new ScriptValue<>(list);
	}
	
	@Override
	public ScriptValue<ScriptValueMap<Object, Object>> visitDictionary(ScriptsParser.DictionaryContext ctx) {
		if (ctx.FOR() == null) {
			ScriptValueMap<Object, Object> map = new ScriptValueMap<>();
			//noinspection SpellCheckingInspection
			Iterator<ScriptsParser.ExpressionContext> iter = ctx.expression().iterator();
			while (iter.hasNext()) {
				map.put((ScriptValue<Object>) visitExpression(iter.next()), (ScriptValue<Object>) visitExpression(iter.next()));
			}
			return new ScriptValue<>(map);
		}
		ScriptValueMap<Object, Object> map = new ScriptValueMap<>();
		ScriptsParser.ComparisonContext ifStatement = ctx.comparison();
		ScriptValue<?> exprToIterate = visitExpression(ctx.expression(2));
		Iterator<ScriptValue<?>> iterator = exprToIterate.iterator(ctx.getText());
		while (iterator.hasNext()) {
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false);
			if (determineBoolean(visitComparison(ifStatement)))
				map.put((ScriptValue<Object>) visitExpression(ctx.expression(0)), (ScriptValue<Object>) visitExpression(ctx.expression(1)));
		}
		return new ScriptValue<>(map);
	}
	
	@Override
	public ScriptValue<?> visitComparison(ScriptsParser.ComparisonContext ctx) {
		ScriptValue<?> before = visitComp_molecule(ctx.comp_molecule(0));
		for (int i = 1; i < ctx.comp_molecule().size(); i++) {
			before = new ScriptValue<>(executeIf(before, ctx.operator.get(i-1), ctx.comp_molecule(i)));
			/*
			if (ctx.operator.get(i-1).getType() == ScriptsParser.AND)
				before = new ScriptValue<>(before.asBoolean() && visitComp_molecule(ctx.comp_molecule(i)).asBoolean());
			else
				before = new ScriptValue<>(before.asBoolean() || visitComp_molecule(ctx.comp_molecule(i)).asBoolean());
			 */
				
		}
		return before;
	}

	public boolean executeIf(@NotNull ScriptsParser.Comp_moleculeContext a, @NotNull Token andOr, @NotNull ScriptsParser.Comp_moleculeContext b) {
		return executeIf(determineBoolean(visitComp_molecule(a)), andOr, b);
	}
	public boolean executeIf(ScriptValue<?> a, @NotNull Token andOr, @NotNull ScriptsParser.Comp_moleculeContext b) {
		return executeIf(determineBoolean(a), andOr, b);
	}
	public boolean executeIf(boolean a, @NotNull Token andOr, @NotNull ScriptsParser.Comp_moleculeContext b) {
		if (andOr.getType() == ScriptsParser.AND) {
			if (!a)
				return false;
			return determineBoolean(visit(b));
		}
		if (a)
			return true;
		return determineBoolean(visit(b));
	}
	
	@Override
	public ScriptValue<?> visitComp_molecule(ScriptsParser.Comp_moleculeContext ctx) {
		ScriptValue<?> before = visitComp_atom(ctx.comp_atom(0)); // will be something first then boolean
		ScriptValue<?> realBefore = before; // fine as before is modified after
		for (int i = 1; i < ctx.comp_atom().size(); i++) {
			switch (ctx.operator.get(i-1).getType()) {
				case ScriptsParser.EQUALS:
					before = new ScriptValue<>(before.equals(visitComp_atom(ctx.comp_atom(i))));
					break;
				case ScriptsParser.NOT_EQUALS:
					before = new ScriptValue<>(! before.equals(visitComp_atom(ctx.comp_atom(i))));
					break;
				case ScriptsParser.GREATER_OR_EQUALS:
					before = new ScriptValue<>(determineBoolean(before) && realBefore.asDouble() >= visitComp_atom(ctx.comp_atom(i)).asDouble());
					break;
				case ScriptsParser.LESSER_OR_EQUALS:
					before = new ScriptValue<>(determineBoolean(before) && realBefore.asDouble() <= visitComp_atom(ctx.comp_atom(i)).asDouble());
					break;
				case ScriptsParser.GREATER:
					before = new ScriptValue<>(determineBoolean(before) && realBefore.asDouble() > visitComp_atom(ctx.comp_atom(i)).asDouble());
					break;
				case ScriptsParser.LESSER:
					before = new ScriptValue<>(determineBoolean(before) && realBefore.asDouble() < visitComp_atom(ctx.comp_atom(i)).asDouble());
					break;
				case ScriptsParser.IN:
					switch (before.type) {
						case "List":
							before = new ScriptValue<>(before.asList().contains(visitComp_atom(ctx.comp_atom(i))));
							break;
						case "Dictionary":
							before = new ScriptValue<>(before.asMap().containsKey(visitComp_atom(ctx.comp_atom(i))));
							break;
						case "Text":
							before = new ScriptValue<>(before.asString().contains(visitComp_atom(ctx.comp_atom(i)).asString()));
							break;
						default:
							throw new ScriptException(ExceptionType.NOT_A_CONTAINER, "\"in\"", ctx.getText(), before.v + " is not a container, you can't check if something is or isn't in it. Containers are List, Dictionary and Text.");
					}
					break;
				default:
					CustomLogger.severe("not a valid operator: " + ctx.operator.get(i-1).getText());
			}
			if (before.v.equals(false)) // v should never be null
				return new ScriptValue<>(false);
		}
		return before;
	}
	
	@Override
	public ScriptValue<?> visitComp_atom(ScriptsParser.Comp_atomContext ctx) {
		ScriptValue<?> expr;
		if (ctx.expression_part() != null)
			expr = visitExpression_part(ctx.expression_part());
		else if (ctx.expression() != null)
			expr = visitExpression(ctx.expression());
		else if (ctx.maths() != null)
			expr = visitMaths(ctx.maths());
		else
			expr = visitLog(ctx.log());
			
		if (ctx.NOT() != null)
			return new ScriptValue<>(! determineBoolean(expr));
		return expr;
	}
	
	@Override
	public ScriptValue<?> visitMaths(ScriptsParser.MathsContext ctx) {
		ScriptValue<?> before = visitMultiplication(ctx.multiplication(0));
		for (int i = 1; i < ctx.multiplication().size(); i++) {
			before = calculate(before, ctx.operator.get(i-1), visitMultiplication(ctx.multiplication(i)),
					ctx.multiplication(i-1).getText() + " " + ctx.operator.get(i-1).getText() + " " + ctx.multiplication(i).getText(), ctx.multiplication(i-1).getText());
		}
		return before;
	}
	
	@Override
	public ScriptValue<?> visitMultiplication(ScriptsParser.MultiplicationContext ctx) {
		ScriptValue<?> before = visitPow(ctx.pow(0));
		for (int i = 1; i < ctx.pow().size(); i++) {
			before = calculate(before, ctx.operator.get(i-1), visitPow(ctx.pow(i)),
					ctx.pow(i-1).getText() + " " + ctx.operator.get(i-1).getText() + " " + ctx.pow(i).getText(), ctx.pow(i-1).getText());
		}
		return before;
	}
	
	@Override
	public ScriptValue<?> visitPow(ScriptsParser.PowContext ctx) {
		if (ctx.rigth1 != null) {
			return calculate(visitMaths_atom(ctx.left), ctx.POW().getSymbol(), visitMaths_atom(ctx.rigth1), ctx.getText(), ctx.left.getText());
		}
		if (ctx.rigth2 != null) {
			return calculate(visitMaths_atom(ctx.left), ctx.POW().getSymbol(), visitPow(ctx.rigth2), ctx.getText(), ctx.left.getText());
		}
		return visitMaths_atom(ctx.left);
	}
	
	@Override
	public ScriptValue<?> visitMaths_atom(ScriptsParser.Maths_atomContext ctx) {
		ScriptValue<?> value;
		if (ctx.MINUS() != null) {
			value = visitMaths_atom(ctx.maths_atom());
			if (value.is("Integer"))
				value = new ScriptValue<>(-value.asInteger());
			else if (value.is("Decimal"))
				value = new ScriptValue<>(-value.asDouble());
			else
				throw ScriptException.invalidType("[negate] ", "Integer or Decimal for negating it", Types.getPrettyArg(value), value.type);
		}
		else if (ctx.expression_part() != null)
			value = visitExpression_part(ctx.expression_part());
		else if (ctx.expression() != null)
			value = visitExpression(ctx.expression());
		else // log
			value = visitLog(ctx.log());
		
		return value;
	}

	@SuppressWarnings("SpellCheckingInspection") // yes, just for one comment word
	@Override
	public ScriptValue<?> visitMini_if(ScriptsParser.Mini_ifContext ctx) { // easy peasy
		if (visitComparison(ctx.comparison()).asBoolean())
			return visitExpression(ctx.yes);
		return visitExpression(ctx.no);
	}
	
	public static boolean determineBoolean(ScriptValue<?> value) {
		switch (value.type) {
			case "Boolean":
				return value.asBoolean();
			case "Integer":
				return value.asInteger() != 0;
			case "Decimal":
				return value.asDouble() != 0;
			case "List":
				return value.asList().size() != 0;
			case "Dictionary":
				return value.asMap().size() != 0;
			case "Text":
				return value.asString().length() != 0;
			default:
				return false;
		}
	}
	
	@Override
	public ScriptValue<?> visitLog(ScriptsParser.LogContext ctx) {
		ScriptValue<?> value = visitExpression_part(ctx.expression_part());
		CustomLogger.info("[script's log] " + ctx.expression_part().getText() + " = " + Types.getPrettyArg(value));
		return value;
	}

	@Override
	public ScriptValue<?> visitAssertion(ScriptsParser.AssertionContext ctx) {
		if (! determineBoolean(visitComparison(ctx.comparison()))) {
			String message = "no further information provided in the \"assert\" statement";
			if (ctx.expression() != null)
				message = visitExpression(ctx.expression()).toString();
			throw new ScriptException(ExceptionType.ASSERTION_ERROR, "assert", ctx.comparison().getText(), message);
		}
		return NONE;
	}

	@Override
	public ScriptValue<String> visitFormatted_text(ScriptsParser.Formatted_textContext ctx) {
		return visitF_text_inner(ctx.f_text_inner());
	}
	
	@Override
	public ScriptValue<String> visitF_text_inner(ScriptsParser.F_text_innerContext ctx) {
		StringBuilder text = new StringBuilder(visitF_text_chars(ctx.f_text_chars(0)).asString());
		for (int i = 0; i < ctx.f_text_placeholder().size(); i++) {
			text.append(visitF_text_placeholder(ctx.f_text_placeholder(i)).toString());
			text.append(visitF_text_chars(ctx.f_text_chars(i + 1)).asString());
		}

		return new ScriptValue<>(text.toString());
	}
	
	@Override
	public ScriptValue<String> visitF_text_chars(ScriptsParser.F_text_charsContext ctx) {
		StringBuilder text = new StringBuilder();
		for (Token token: ctx.chars) {
			if (token.getText().equals("{=/"))
				text.append("{=");
			else
				text.append(token.getText());
		}
		return new ScriptValue<>(text.toString());
	}
	
	@Override
	public ScriptValue<?> visitF_text_placeholder(ScriptsParser.F_text_placeholderContext ctx) {
		return visitExpression(ctx.expression());
	}

	private @NotNull
	static String toEmptyStringIfNull(@Nullable Object obj) {
		if (obj == null) return "";
		return obj.toString();
	}
	
}