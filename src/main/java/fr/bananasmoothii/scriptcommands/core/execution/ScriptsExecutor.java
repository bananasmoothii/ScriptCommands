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
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ContextStackTraceElement;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import static fr.bananasmoothii.scriptcommands.core.execution.ScriptValue.NONE;
import static fr.bananasmoothii.scriptcommands.core.execution.ScriptValue.ScriptValueType;

@SuppressWarnings("unchecked") // because we are often switching from wildcard to object
public class ScriptsExecutor extends ScriptsParserBaseVisitor<ScriptValue<?>> { // should only implement ScriptsVisitor

	
	protected final Context context;

	private @NotNull ScriptValue<?> returned = NONE;
	private boolean hasReturned;
	private @Nullable Consumer<? super ScriptValue<?>> onReturn;
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

	/**
	 * @return the return value of the script if it reached a "return" statement, {@link ScriptValue#NONE} otherwise.
	 * @see #hasReturned()
	 */
	public @NotNull ScriptValue<?> getReturned() {
		return returned;
	}

	/**
	 * @return whether the script reached a "return" statement
	 * @see #getReturned()
	 */
	public boolean hasReturned() {
		return hasReturned;
	}

	/**
	 * Runs "action" with the returned {@link ScriptValue} when a "return" block has been reached (this happens only once).
	 * You can add as many actions as you want, it will use {@link Consumer#andThen(Consumer)}
	 */
	public void onReturn(Consumer<? super ScriptValue<?>> action) {
		if (onReturn == null) onReturn = action;
		else onReturn = onReturn.andThen((Consumer<Object>) action);
	}

	public void forceReturn() {
		this.breaking = 'r';
	}

	@Override
	public ScriptValue<?> visit(ParseTree tree) {
		if (breaking != 'n') return NONE;
		if (hasReturned) throw new IllegalStateException("cannot run anything if a \"return\" statement has been reached.");
		return super.visit(tree);
	}
	
	/**
	 * Start for parsing. It catches any {@link RuntimeException} and prints them.
	 */
	@Override
	public ScriptValue<NoneType> visitStart(ScriptsParser.StartContext ctx) {
		try {
			super.visitStart(ctx);
		} catch (RuntimeException e) {
			e.printStackTrace();
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

	@Override
	public ScriptValue<NoneType> visitFor_block(ScriptsParser.For_blockContext ctx) {
		Iterator<ScriptValue<?>> iterator = null;
		if (ctx.expression().expression_part().function().size() == 1 &&
				ctx.expression().expression_part().function(0).get_from_list().isEmpty())
			iterator = Context.getIterator(ctx.expression().expression_part().function(0).VARIABLE().getText(),
					visitArgs(ctx.expression().expression_part().function(0).arguments()));
		if (iterator == null) {
			ScriptValue<?> exprToIterate = visitExpression(ctx.expression());
			iterator = exprToIterate.iterator(context, new ScriptException.ScriptStackTraceElement(context,
					ctx.FOR().getText() + " " + ctx.VARIABLE().getText() + " " + ctx.IN().getText() + " " +
							ctx.expression().getText() + " {...}",
					ctx.start));
		}
		while (iterator.hasNext()) {
			if (breaking == 'c')
				breaking = 'n';
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false, ctx.start.getLine(), ctx.start.getCharPositionInLine());
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
		} catch (RuntimeException e) {
			if (e instanceof ScriptException.Incomplete) e = ((ScriptException.Incomplete) e).complete(context);
			if (! (e instanceof ScriptException)) e = ScriptException.wrapInShouldNotHappen(e, context);
			for (ScriptsParser.Catch_blockContext catchCtx: ctx.catch_block()) {
				for (ScriptsParser.ExpressionContext expression : catchCtx.expression()) {
					ScriptValue<?> expr = visitExpression(expression);
					if (!expr.is(ScriptValueType.TEXT) && !expr.is(ScriptValueType.BOOLEAN)) {
						throw ScriptException.invalidType("Text or Boolean", expr,
								new ContextStackTraceElement(context,"try {...} catch", ctx.start));
					}
					if (expr.is(ScriptValueType.TEXT) && expr.asString().equals(((AbstractScriptException) e).getStringType())
							|| (expr.is(ScriptValueType.BOOLEAN) && expr.asBoolean())) {
						if (catchCtx.varToAssign != null)
							context.assign(catchCtx.varToAssign.getText(), expr, false, catchCtx.start.getLine(), catchCtx.start.getCharPositionInLine());
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
		hasReturned = true;
		breaking = 'r';
		if (onReturn != null) onReturn.accept(returned);
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
	public ScriptValue<String> visitThread_expression(ScriptsParser.Thread_expressionContext ctx) {
		boolean presentId = ctx.expression().size() == 2;
		String threadName = presentId ? visitExpression(ctx.expression(0)).asString() : null;
		ScriptsParser.ExpressionContext ctxToExecute = presentId ? ctx.expression(1) : ctx.expression(0);
		ScriptThread scriptThread;
		if (ctx.IN() != null) {
			//noinspection ConstantConditions because if IN is present, there has to be that expression according to the ANTLR4 grammar
			scriptThread = new ScriptThread(ctxToExecute, context, threadName);
		} else {
			scriptThread = new ScriptThread(threadName, ctxToExecute, context);
		}
		scriptThread.start();
		return new ScriptValue<>(scriptThread.getThreadName());
	}

	@Override
	public ScriptValue<String> visitThread_block(ScriptsParser.Thread_blockContext ctx) {
		String threadName = ctx.expression() != null ? visitExpression(ctx.expression()).asString() : null;
		ScriptsParser.BlockContext ctxToExecute = ctx.block();
		ScriptThread scriptThread;
		if (ctx.IN() != null) {
			//noinspection ConstantConditions because if IN is present, there has to be that expression according to the ANTLR4 grammar
			scriptThread = new ScriptThread(ctxToExecute, context, threadName);
		} else {
			scriptThread = new ScriptThread(threadName, ctxToExecute, context);
		}
		scriptThread.start();
		return new ScriptValue<>(scriptThread.getThreadName());
	}

	@Override
	public ScriptValue<?> visitThrow_(ScriptsParser.Throw_Context ctx) {
		throw new ScriptException(visitExpression(ctx.expression(0)).asString(),
				ctx.expression().size() == 2 ? visitExpression(ctx.expression(1)).asString() : "<no error message provided>",
				new ContextStackTraceElement(context,
						"throw " + ctx.expression(0).getText(),
						ctx.start));
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
			args = new Args(context);
		if (arg != null)
			args.add(0, (ScriptValue<Object>) arg);

		ScriptValue<?> executed = context.callAndRun(ctx.VARIABLE().getText(), args);
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			executed = get_from_list(executed, getFromListContext, ctx.getText());
		}
		return executed;
	}

	public Args visitArgs(ScriptsParser.ArgumentsContext ctx) {
		Args args = new Args(context);
		for (ScriptsParser.ExpressionContext exprCtx: ctx.expression()) {
			args.add((ScriptValue<Object>) visitExpression(exprCtx));
		}
		for (ScriptsParser.SimpleAssignmentContext simpleAssignmentContext : ctx.simpleAssignment()) {
			args.add(simpleAssignmentContext.VARIABLE().getText(), (ScriptValue<Object>) visitExpression(simpleAssignmentContext.expression()));
		}
		for (ScriptsParser.SplatListContext splatCtx: ctx.splatList()) {
			ScriptValue<?> visited = visitSplatList(splatCtx);
			if ( ! visited.is(ScriptValueType.LIST))
				throw ScriptException.invalidType("List", visited,
						new ContextStackTraceElement(context, "splat list: " + splatCtx.getText(), splatCtx.start));
		}
		for (ScriptsParser.SplatDictContext splatCtx: ctx.splatDict()) {
			ScriptValue<?> visited = visitSplatDict(splatCtx);
			if ( ! visited.is(ScriptValueType.DICTIONARY))
				throw ScriptException.invalidType("Dictionary", visited,
						new ContextStackTraceElement(context, "splat dictionary: " + splatCtx.getText(), splatCtx.start));

			StringScriptValueMap<Object> argsMap = args.getArgsMap();
			for (Map.Entry<ScriptValue<Object>, ScriptValue<Object>> entry: visited.asMap().entrySet(context)) {
				if (! entry.getKey().is(ScriptValueType.TEXT))
					throw ScriptException.invalidType("a Dictionary with only strings as keys", entry.getKey().toString(),
							new ContextStackTraceElement(context, "splat dictionary : " + splatCtx.getText(), splatCtx.start));
				argsMap.put(entry.getKey().asString(), entry.getValue(), context);
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
	
	public ScriptValue<?> calculate(ScriptValue<?> a, Token operator, ScriptValue<?> b, String where) {
		return calculate(a, operator, b, where, false);
	}

	/**
	 * @param global just used to thrown an error if that doesn't mean anything
	 */
	// todo for the documentation: a table with the description of what will an operator do with two types
	public ScriptValue<?> calculate(ScriptValue<?> a, Token operator, ScriptValue<?> b, String where, boolean global) {
		ScriptException error = new ScriptException(ExceptionType.INVALID_OPERATOR, context, "Invalid operator '" +
				operator.getText() + "' between " + a.type.name + " and " + b.type.name,
				new ContextStackTraceElement(context, a + operator.getText() + b, operator));
		ScriptException globalError = new ScriptException(ExceptionType.GLOBAL_NOT_ALLOWED, "operator '" +
				operator.getText() + "' between " + a.type.name + " and " + b.type.name + " cannot be used " +
				"along with the keyword \"global\", that doesn't mean anything.", new ContextStackTraceElement(context,
				where, operator));
		switch (operator.getType()) {
			case ScriptsParser.PLUS:
				if (a.is(ScriptValueType.INTEGER) && b.is(ScriptValueType.INTEGER))
					return new ScriptValue<>(a.asInteger() + b.asInteger());
				else if (a.isNumber() && b.isNumber())
					return new ScriptValue<>(a.asDouble() + b.asDouble());
				else if ((a.is(ScriptValueType.TEXT) && (b.isNumber() || b.is(ScriptValueType.TEXT))) || (b.is(ScriptValueType.TEXT) && a.isNumber()))
					return new ScriptValue<>(a.toString() + b);
				else {
					if (global)
						throw globalError;
					if (a.is(ScriptValueType.LIST)) {
						a.asList().add((ScriptValue<Object>) b, context);
						return NONE;
					}
					else if (a.is(ScriptValueType.DICTIONARY) && b.is(ScriptValueType.LIST)) {
						if (b.asList().size(context) != 2)
							throw error;
						a.asMap().put(b.asList().get(0, context), b.asList().get(1, context), context);
						return NONE;
					}
					else
						throw error;
				}
			case ScriptsParser.MINUS:
				if (a.is(ScriptValueType.INTEGER) && b.is(ScriptValueType.INTEGER))
					return new ScriptValue<>(a.asInteger() - b.asInteger());
				else if (a.isNumber() && b.isNumber())
					return new ScriptValue<>(a.asDouble() - b.asDouble());
				else {
					if (global)
						throw globalError;
					if (a.is(ScriptValueType.LIST)) {
						if (! b.is(ScriptValueType.INTEGER))
							a.asList().remove(b, context);
						else
							a.asList().remove(b.asInteger(), context);
						return NONE;
					}
					else if (a.is(ScriptValueType.DICTIONARY)) {
						a.asMap().remove(b, context);
						return NONE;
					}
					else throw error;
				}
			case ScriptsParser.TIMES:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is(ScriptValueType.INTEGER) && b.is(ScriptValueType.INTEGER))
					return new ScriptValue<>(a.asInteger() * b.asInteger());
				else
					return new ScriptValue<>(a.asDouble() * b.asDouble());
			case ScriptsParser.DIVIDE:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				return new ScriptValue<>(a.asDouble() / b.asDouble());
			case ScriptsParser.FLOOR_DIVIDE:
				return new ScriptValue<>((int) (a.asDouble() / b.asDouble()));
			case ScriptsParser.MODULO:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is(ScriptValueType.INTEGER) && b.is(ScriptValueType.INTEGER))
					return new ScriptValue<>(a.asInteger() % b.asInteger());
				else
					return new ScriptValue<>(a.asDouble() % b.asDouble());
			case ScriptsParser.POW:
				if (!(a.isNumber()&&b.isNumber())) throw error;
				if (a.is(ScriptValueType.INTEGER) && b.is(ScriptValueType.INTEGER))
					return new ScriptValue<>((int) Math.pow(a.asInteger(), b.asInteger()));
				else
					return new ScriptValue<>(Math.pow(a.asDouble(), b.asDouble()));
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
			context.assign(varName,
					calculate(varValue, ctx.operator, value.clone(), ctx.getText(), global), // same here (for the clone)
					global,
					ctx.start);
		}
		else
			context.assign(varName, value.clone(), global, ctx.start); // TODO: check if the "clone" is a wanted feature or just a bug
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitBlock_assignment(ScriptsParser.Block_assignmentContext ctx) {
		String varName = ctx.VARIABLE().getText();
		ScriptValue<?> value = visitThread_block(ctx.value);
		boolean global = ctx.GLOBAL() != null;

		if (ctx.operator != null) {
			ScriptValue<?> varValue = context.callAndRun(varName);
			context.assign(varName,
					calculate(varValue, ctx.operator, value, ctx.getText(), global),
					global,
					ctx.start);
		}
		else
			context.assign(varName, value, global, ctx.start); // no need to clone here
		return NONE;
	}

	@Override
	public ScriptValue<NoneType> visitDeletion(ScriptsParser.DeletionContext ctx) {
		context.delete(ctx.VARIABLE().getText()); // easy :D
		return NONE;
	}
	
	protected ScriptValue<?> get_from_list(ScriptValue<?> obj, ScriptsParser.Get_from_listContext getFromListContext, String scriptCause) {
		if (getFromListContext.nbSingle != null) {
			if (obj.is(ScriptValueType.LIST)) {
				int number = visitExpression(getFromListContext.nbSingle).asInteger();
				int size = obj.asList().size(context);
				if (number >= size || number <= -size)
					throw getOutOfBoundsException(scriptCause, number, size, getFromListContext.start);
				if (number < 0) number += size;
				return obj.asList().get(number, context);
			}
			else if (obj.is(ScriptValueType.DICTIONARY)) {
				ScriptValue<?> key = visitExpression(getFromListContext.nbSingle);
				try {
					return obj.asMap().get(key, context);
				} catch (ScriptException.Incomplete e) {
					throw e.complete(new ContextStackTraceElement(context, scriptCause, getFromListContext.start));
				}
			}
			else if (obj.is(ScriptValueType.TEXT)) {
				int number = visitExpression(getFromListContext.nbSingle).asInteger();
				int size = obj.asString().length();
				if (number >= size || number <= -size)
					throw getOutOfBoundsException(scriptCause, number, size, getFromListContext.start);
				if (number < 0) {
					number += size;
				}
				return new ScriptValue<>(String.valueOf(obj.asString().charAt(number)));
			}
			else
				throw new ScriptException(ExceptionType.NOT_LISTABLE, scriptCause +
						" isn't a List, a Dictionary or Text, so you can't ask for a specific element in it",
						new ContextStackTraceElement(context, scriptCause, getFromListContext.start));
		}
		else {
			if (obj.is(ScriptValueType.LIST)) {
				Integer nb1 = getFromListContext.nb1 == null ? null : visitExpression(getFromListContext.nb1).asInteger();
				Integer nb2 = getFromListContext.nb2 == null ? null : visitExpression(getFromListContext.nb2).asInteger();
				if (nb1 == null && nb2 == null)
					return obj;
				int size = obj.asList().size(context);
				if (nb1 != null && (nb1 >= size || nb1 <= -size)) {
					throw getOutOfBoundsException(scriptCause, nb1, size, getFromListContext.start);
				}
				if (nb2 != null && (nb2 >= size || nb2 <= -size))
					throw getOutOfBoundsException(scriptCause, nb2, size, getFromListContext.start);
				if (nb1 != null) {
					if (nb1 < 0)
						nb1 += size;
						
					if (nb2 != null) {
						if (nb2 < 0)
							nb2 += size;
						return new ScriptValue<>(obj.asList().subList(nb1, nb2));
					}
					else {
						return new ScriptValue<>(obj.asList().subList(nb1, obj.asList().size(context)));
					}
				}
				else {
					if (nb2 < 0)
						nb2 += size;
					return new ScriptValue<>(obj.asList().subList(0, nb2));
				}
			}
			else if (obj.is(ScriptValueType.TEXT)) {
				boolean noNb1 = false, noNb2 = false;
				int nb1 = 0, nb2 = 0;

				if (getFromListContext.nb1 == null) noNb1 = true;
				else nb1 = visitExpression(getFromListContext.nb1).asInteger();

				if (getFromListContext.nb2 == null) noNb2 = true;
				else nb2 = visitExpression(getFromListContext.nb2).asInteger();

				if (noNb1 && noNb2)
					return obj;
				int size = obj.asString().length();
				if (!noNb1 && (nb1 >= size || nb1 <= -size))
					throw getOutOfBoundsException(scriptCause, nb1, size, getFromListContext.start);
				if (!noNb2 && (nb2 >= size || nb2 <= -size))
					throw getOutOfBoundsException(scriptCause, nb2, size, getFromListContext.start);
				if (!noNb1) {
					if (nb1 < 0)
						nb1 += size;

					if (!noNb2) {
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
				throw new ScriptException(ExceptionType.NOT_LISTABLE, scriptCause +
						" isn't a List, a Dictionary or Text, so you can't ask for a specific element in it",
						new ContextStackTraceElement(context,
								scriptCause + '[' + nb1 + ':' + nb2 + ']', getFromListContext.start));
			}
		}
	}

	@NotNull
	private ScriptException getOutOfBoundsException(String nameOfVar, int index, int size, Token startToken) {
		return new ScriptException(ExceptionType.OUT_OF_BOUNDS, "you asked for element " + index +
				" of List " + nameOfVar + " but it contains only " + size + " elements, so you can ask for element from " +
				(-size + 1) + " to " + (size - 1), new ContextStackTraceElement(context, nameOfVar, startToken));
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
			expr = get_from_list(expr, getFromListContext, ctx.getText());
		}
		return expr;
	}
	
	@Override
	public ScriptValue<ScriptValueList<Object>> visitExprList(ScriptsParser.ExprListContext ctx) {
		ScriptValue<ScriptValueList<Object>> list = visitList(ctx.list());
		for (ScriptsParser.Get_from_listContext getFromListContext: ctx.get_from_list()) {
			list = (ScriptValue<ScriptValueList<Object>>) get_from_list(list, getFromListContext, ctx.getText());
		}
		return list;
	}
	
	@Override
	public ScriptValue<ScriptValueMap<Object, Object>> visitExprDictionary(ScriptsParser.ExprDictionaryContext ctx) {
		ScriptValue<ScriptValueMap<Object, Object>> dict = visitDictionary(ctx.dictionary());
		for (ScriptsParser.Get_from_listContext getFromListContext : ctx.get_from_list()) {
			dict = (ScriptValue<ScriptValueMap<Object, Object>>) get_from_list(dict, getFromListContext, ctx.getText());
		}
		return dict;
	}
	
	@Override
	public ScriptValue<ScriptValueList<Object>> visitList(ScriptsParser.ListContext ctx) {
		if (ctx.FOR() == null) {
			ScriptValueList<Object> list = new ScriptValueList<>();
			for (ScriptsParser.ExpressionContext elemCtx: ctx.expression()) {
				list.add((ScriptValue<Object>) visitExpression(elemCtx), context);
			}
			return new ScriptValue<>(list);
		}
		ScriptValueList<Object> list = new ScriptValueList<>();
		ScriptValue<?> exprToIterate = visitExpression(ctx.expression(1));
		ScriptsParser.ComparisonContext ifStatement = ctx.comparison();
		Iterator<ScriptValue<?>> iterator = exprToIterate.iterator(
				new ContextStackTraceElement(context, ctx.getText(), ctx.start));
		while (iterator.hasNext()) {
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false, ctx.start);
			if (determineBoolean(visitComparison(ifStatement)))
				list.add((ScriptValue<Object>) visitExpression(ctx.expression(0)), context);
		}
		return new ScriptValue<>(list);
	}
	
	@Override
	public ScriptValue<ScriptValueMap<Object, Object>> visitDictionary(ScriptsParser.DictionaryContext ctx) {
		if (ctx.FOR() == null) {
			ScriptValueMap<Object, Object> map = new ScriptValueMap<>();
			Iterator<ScriptsParser.ExpressionContext> iter = ctx.expression().iterator();
			while (iter.hasNext()) {
				map.put((ScriptValue<Object>) visitExpression(iter.next()), (ScriptValue<Object>) visitExpression(iter.next()), context);
			}
			return new ScriptValue<>(map);
		}
		ScriptValueMap<Object, Object> map = new ScriptValueMap<>();
		ScriptsParser.ComparisonContext ifStatement = ctx.comparison();
		ScriptValue<?> exprToIterate = visitExpression(ctx.expression(2));
		Iterator<ScriptValue<?>> iterator = exprToIterate.iterator(
				new ContextStackTraceElement(context, ctx.getText(), ctx.start));

		while (iterator.hasNext()) {
			context.assign(ctx.VARIABLE().getText(), iterator.next(), false, ctx.start);
			if (determineBoolean(visitComparison(ifStatement)))
				map.put((ScriptValue<Object>) visitExpression(ctx.expression(0)), (ScriptValue<Object>) visitExpression(ctx.expression(1)), context);
		}
		return new ScriptValue<>(map);
	}
	
	@Override
	public ScriptValue<?> visitComparison(ScriptsParser.ComparisonContext ctx) {
		ScriptValue<?> before = visitComp_molecule(ctx.comp_molecule(0));
		for (int i = 1; i < ctx.comp_molecule().size(); i++) {
			before = new ScriptValue<>(executeIf(before, ctx.operator.get(i-1), ctx.comp_molecule(i)));
		}
		return before;
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
						case LIST:
							before = new ScriptValue<>(before.asList().contains(visitComp_atom(ctx.comp_atom(i))));
							break;
						case DICTIONARY:
							before = new ScriptValue<>(before.asMap().containsKey(visitComp_atom(ctx.comp_atom(i)), context));
							break;
						case TEXT:
							before = new ScriptValue<>(before.asString().contains(visitComp_atom(ctx.comp_atom(i)).asString()));
							break;
						default:
							throw new ScriptException(ExceptionType.NOT_LISTABLE, before.v +
									" is not a container, you can't check if something is or isn't in it. Containers are List, Dictionary and Text.",
									new ContextStackTraceElement(context, ctx.getText(), ctx.start));
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
					ctx.multiplication(i-1).getText() + " " + ctx.operator.get(i-1).getText() + " " + ctx.multiplication(i).getText());
		}
		return before;
	}
	
	@Override
	public ScriptValue<?> visitMultiplication(ScriptsParser.MultiplicationContext ctx) {
		ScriptValue<?> before = visitPow(ctx.pow(0));
		for (int i = 1; i < ctx.pow().size(); i++) {
			before = calculate(before, ctx.operator.get(i-1), visitPow(ctx.pow(i)),
					ctx.pow(i-1).getText() + " " + ctx.operator.get(i-1).getText() + " " + ctx.pow(i).getText());
		}
		return before;
	}
	
	@Override
	public ScriptValue<?> visitPow(ScriptsParser.PowContext ctx) {
		if (ctx.rigth1 != null) {
			return calculate(visitMaths_atom(ctx.left), ctx.POW().getSymbol(), visitMaths_atom(ctx.rigth1), ctx.getText());
		}
		if (ctx.rigth2 != null) {
			return calculate(visitMaths_atom(ctx.left), ctx.POW().getSymbol(), visitPow(ctx.rigth2), ctx.getText());
		}
		return visitMaths_atom(ctx.left);
	}
	
	@Override
	public ScriptValue<?> visitMaths_atom(ScriptsParser.Maths_atomContext ctx) {
		ScriptValue<?> value;
		if (ctx.MINUS() != null) {
			value = visitMaths_atom(ctx.maths_atom());
			if (value.is(ScriptValueType.INTEGER))
				value = new ScriptValue<>(-value.asInteger());
			else if (value.is(ScriptValueType.DECIMAL))
				value = new ScriptValue<>(-value.asDouble());
			else
				throw ScriptException.invalidType("Integer or Decimal for negating it", value,
						new ContextStackTraceElement(context, ctx.getText(), ctx.start));
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
	
	public boolean determineBoolean(ScriptValue<?> value) {
		switch (value.type) {
			case BOOLEAN:
				return value.asBoolean();
			case INTEGER:
				return value.asInteger() != 0;
			case DECIMAL:
				return value.asDouble() != 0;
			case TEXT:
				return !value.asString().isEmpty();
			case LIST:
				return !value.asList().isEmpty(context);
			case DICTIONARY:
				return !value.asMap().isEmpty(context);
			default:
				return false;
		}
	}
	
	@Override
	public ScriptValue<?> visitLog(ScriptsParser.LogContext ctx) {
		ScriptValue<?> value = visitExpression_part(ctx.expression_part());
		CustomLogger.info("[log] " + ctx.expression_part().getText() + " = " + Types.getPrettyArg(value));
		return value;
	}

	@Override
	public ScriptValue<?> visitAssertion(ScriptsParser.AssertionContext ctx) {
		if (! determineBoolean(visitComparison(ctx.comparison()))) {
			String message = "no further information provided in the \"assert\" statement";
			if (ctx.expression() != null)
				message = visitExpression(ctx.expression()).toString();
			throw new ScriptException(ExceptionType.ASSERTION_ERROR, message,
					new ContextStackTraceElement(context, "assert " + ctx.comparison().getText(), ctx.start));
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
	
}