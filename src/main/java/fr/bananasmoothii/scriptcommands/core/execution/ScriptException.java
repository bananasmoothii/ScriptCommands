/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Objects;

/**
 * The main exception used everywhere in scripts.
 * @see Incomplete if you don't have any Context to provide, or you don't know where the exception happened in the script
 */
public class ScriptException extends AbstractScriptException {

    protected String stringType;
    protected @Nullable String genericErrorDescription;
    protected final @NotNull Context context;
    protected ScriptStackTraceElement[] scriptStackTrace;

    public ScriptException(ExceptionType type, @NotNull Context context, String message) {
        this(type.name(), context, message, null);
        genericErrorDescription = type.description;
    }

    /**
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(ExceptionType type, @NotNull Context context, String message, @Nullable ScriptStackTraceElement where) {
        this(type.name(), context, message, where);
        genericErrorDescription = type.description;
    }

    /**
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(ExceptionType type, String message, @NotNull ContextStackTraceElement where) {
        this(type.name(), where.context, message, where);
        genericErrorDescription = type.description;
    }

    /**
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(String type, String message, @NotNull ContextStackTraceElement where) {
        this(type, where.context, message, where);
    }

    /**
     * Root for all constructors
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(String type, @NotNull Context context, String message, @Nullable ScriptStackTraceElement where) {
        super(message);
        this.stringType = type;
        this.context = context;
        scriptStackTrace = context.getStackTrace(where);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder()
                .append("Script Error \"")
                .append(stringType)
                .append("\": ")
                .append(super.getMessage());
        if (scriptStackTrace.length >= 1) {
            sb.append("\nin ")
              .append(scriptStackTrace[0]);
        }
        for (int i = 1; i < scriptStackTrace.length; i++) {
            sb.append("\ncaused by ")
              .append(scriptStackTrace[i]);
        }
        if (genericErrorDescription != null) {
            sb.append('\n')
              .append(genericErrorDescription);
        }
        sb.append("\nThe following is the Java stack trace, that might help me as plugin dev if there is a bug.");
        return sb.toString();
    }

    @Override
    public String getStringType() {
        return stringType;
    }

    @Override
    public void setStringType(String stringType) {
        this.stringType = Objects.requireNonNull(stringType);
    }

    @Override
    public @Nullable String getGenericErrorDescription() {
        return genericErrorDescription;
    }

    @Override
    public @NotNull ScriptException setGenericErrorDescription(@Nullable String genericErrorDescription) {
        this.genericErrorDescription = genericErrorDescription;
        return this;
    }

    public @NotNull Context getContext() {
        return context;
    }

    public ScriptStackTraceElement[] getScriptStackTrace() {
        return scriptStackTrace;
    }

    public void setScriptStackTrace(ScriptStackTraceElement[] scriptStackTrace) {
        this.scriptStackTrace = scriptStackTrace;
    }

    // TODO: transform to classes so third-party programs can create new errors
    public enum ExceptionType {
        ASSERTION_ERROR("An assertion error happens when when using the \"assert\" keyword but the expression " +
                "evaluates to false."),
        CONVERSION_ERROR("A conversion error happens when you want to convert a type to another. For example, " +
                "you can convert \"12.8\" (a string) to 12.8 (a decimal number), and 12.8 to an integer (it will be " +
                "rounded to 12), but you can't convert a list to a boolean or whatever the heck you are thinking about " +
                "(just joking you're intelligent <3)."),
        GLOBAL_NOT_ALLOWED("Global not allowed means that you can't use the \"global\" keyword where you just used it."),
        INVALID_ARGUMENTS("Invalid arguments happens when you don't give the right arguments to a function."),
        INVALID_OPERATOR("Invalid operator happens when the operator you are using doesn't work with the two " +
                "values you put in, for example you can't subtract a list to a boolean (what would that mean ??)"),
        INVALID_TYPE("Invalid type happens when the type you are using doesn't work with the function you used " +
                "it with. This happens very often in functions, but it can also show up in iterations (for i in ...), " +
                "with the splat operator * not on a list or ** not on a dictionary, "),
        NOT_DEFINED("Not defined is an error happening when you are calling a variable or a function that is " +
                "not defined, or you are calling an element in a dictionary that doesn't exist (e.g. [=][\"something\"] )."),
        NOT_LISTABLE("Not listable happens if you are using the 'in' keyword to check if an element is in a " +
                "list/dictionary/text, but the last part is not a list (e.g. when you do something like if 10 in 12 {...}"),
        NOT_OVERRIDABLE("Not overridable shows up when you try to create a variable but it already exists as " +
                "function, or when you are creating a global variable but it already exists as non-global variable."),
        OUT_OF_BOUNDS("Out of bounds just means you called an element in a list but your index was equal or " +
                "above the number of elements in the list (remember that list indexes start at 0), or the negative " +
                "equivalent. For example, if you have the list [\"a\", \"b\", \"c\"], you can call elements 0 " +
                "(-> \"a\"), 1 (-> \"b\"), 2 (-> \"c\"), or negative index -1 (-> \"c\"), -2 (-> \"b\"), -3 (-> \"a\"). " +
                "For a list of 3 elements, these are the only 6 indexes that will not cause an OUT_OF_BOUNDS error."),
        PARSING_ERROR("A parsing error happens with eval() or exec(). These two functions are should not be used " +
                "for more security, code readability and performance, but they are still here in case you found no other " +
                "way. The code you provided to one of these function is not valid. Remember that eval() accepts only an " +
                "expression, not full lines of code. For example, here is what is an expression: log foo.bar(foo1, bar1) ; " +
                "4 * a ; 4 > a ; 4 > a ? \"a is small\" else \"a is big\" . Here is what is not an expression: if, for, " +
                "thread, while, try..."),
        SHOULD_NOT_HAPPEN("Hey ! You got a Should Not Happen error ! As the name states it, this error should " +
                "never happen and it is a bug. Please report it at https://github.com/bananasmoothii/ScriptCommands/issues ."),
        UNAVAILABLE_THREAD_NAME("Unavailable thread name means that you wanted to make a new thread with a name " +
                "that already exists."),
        THREAD_GROUP_ERROR("A thread group error can happen either when you are trying to make a new thread " +
                "with a group that does not exist (like `thread in \"group_that_does_not_exist\" ...`), or when you create " +
                "a new thread group with a name that already exists (like `TODO: write this section`), or when you try " + // TODO: write these
                "to modify a thread group that does not exist (like `TODO: write this section too`)̀.");

        public final String description;

        ExceptionType(String description) {
            this.description = description;
        }
    }

    public static class ScriptStackTraceElement {
        public final Context context;
        public final @Nullable String scriptCause;
        public final @Nullable String args;
        public final int lineNumber, columnNumber;

        /**
         * @param scriptCause the name of the function/variable that caused this error, or the (short) piece of code that
         *                 caused this error, e.g. "18 / 0" or "true[false]". If this stack trace element isn't at the
         *                 top (variable was not the last thing called, but it led to another thing that caused an error),
         *                 this should really point a function with non-null "args" because only a function can
         *                 make the stack trace deeper.
         * @param args the arguments provided to variable
         */
        public ScriptStackTraceElement(@Nullable Context context, @Nullable String scriptCause, @Nullable String args, int lineNumber, int columnNumber) {
            this.context = context;
            this.scriptCause = scriptCause;
            this.args = args;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        /**
         * @see #ScriptStackTraceElement(Context, String, String, int, int)
         */
        public ScriptStackTraceElement(@Nullable Context context, @Nullable String scriptCause, @NotNull Token startToken) {
            this(context, scriptCause, (String) null, startToken.getLine(), startToken.getCharPositionInLine());
        }

        public ScriptStackTraceElement(@Nullable Context context, @Nullable String scriptCause, @Nullable Args args, int lineNumber, int columnNumber) {
            this(context, scriptCause, args != null ? args.toString() : null, lineNumber, columnNumber);
        }

        public ScriptStackTraceElement(@Nullable Context context, @Nullable String scriptCause, int lineNumber, int columnNumber) {
            this(context, scriptCause, (String) null, lineNumber, columnNumber);
        }

        private ScriptStackTraceElement() {
            context = null;
            scriptCause = null;
            args = null;
            lineNumber = 0;
            columnNumber = 0;
        }
        
        public static final ScriptStackTraceElement UNKNOWN = new ScriptStackTraceElement();

        @Override
        public String toString() {
            if (scriptCause == null) return "UNKNOWN SOURCE";
            return scriptCause + ( args != null ? '(' + args + ')' : "") + (lineNumber != 0 && columnNumber != 0 ? " line " + lineNumber + ':' + columnNumber : "") +
                    (context != null ? "in script \"" + context.scriptType + "\" (" + context.scriptType + ')' : "");
        }
    }

    /**
     * Basically the same as super but here, context can't be null
     */
    public static class ContextStackTraceElement extends ScriptStackTraceElement {

        /**
         * @param scriptCause the name of the function/variable that caused this error, or the (short) piece of code that
         *                 caused this error, e.g. "18 / 0" or "true[false]". If this stack trace element isn't at the
         *                 top (variable was not the last thing called, but it led to another thing that caused an error),
         *                 this should really point a function with non-null "args" because only a function can
         *                 make the stack trace deeper.
         * @param args the arguments provided to variable
         */
        public ContextStackTraceElement(@NotNull Context context, @Nullable String scriptCause, @Nullable String args, int lineNumber, int columnNumber) {
            super(Objects.requireNonNull(context), scriptCause, args, lineNumber, columnNumber);
        }

        /**
         * @see #ContextStackTraceElement(Context, String, String, int, int)
         */
        public ContextStackTraceElement(@NotNull Context context, @Nullable String scriptCause, @NotNull Token startToken) {
            super(Objects.requireNonNull(context), scriptCause, startToken);
        }

        /**
         * @see #ContextStackTraceElement(Context, String, String, int, int)
         */
        public ContextStackTraceElement(@NotNull Context context, @Nullable String scriptCause, @Nullable Args args, int lineNumber, int columnNumber) {
            super(Objects.requireNonNull(context), scriptCause, args, lineNumber, columnNumber);
        }

        /**
         * @see #ContextStackTraceElement(Context, String, String, int, int)
         */
        public ContextStackTraceElement(@NotNull Context context, @Nullable String scriptCause, int lineNumber, int columnNumber) {
            super(Objects.requireNonNull(context), scriptCause, lineNumber, columnNumber);
        }
    }


    /**
     * This is an incomplete {@link ScriptException}, for when you don't have any {@link Context} to provide or you
     * don't know where the error happened in the script
     * @see ScriptException
     */
    public static class Incomplete extends AbstractScriptException {

        protected String stringType;
        protected @Nullable String genericErrorDescription;
        protected @Nullable ScriptStackTraceElement where;

        public Incomplete(ExceptionType type, String message) {
            this(type.name(), message, null);
            genericErrorDescription = type.description;
        }

        public Incomplete(ExceptionType type, String message, @Nullable ScriptStackTraceElement where) {
            this(type.name(), message, where);
        }

        public Incomplete(String stringType, String message) {
            this(stringType, message, null);
        }

        public Incomplete(String stringType, String message, @Nullable ScriptStackTraceElement where) {
            super(message);
            this.stringType = stringType;
            this.where = where;
        }

        @Override
        public String getStringType() {
            return stringType;
        }

        @Override
        public void setStringType(String stringType) {
            this.stringType = Objects.requireNonNull(stringType);
        }

        @Override
        public @Nullable String getGenericErrorDescription() {
            return genericErrorDescription;
        }

        @Override
        public @NotNull Incomplete setGenericErrorDescription(@Nullable String genericErrorDescription) {
            this.genericErrorDescription = genericErrorDescription;
            return this;
        }



        public ScriptException complete(@NotNull Context context) {
            return complete(context, where);
        }

        public ScriptException complete(@NotNull ContextStackTraceElement where) {
            return complete(where.context, where);
        }

        public ScriptException complete(@NotNull Context context, @Nullable ScriptStackTraceElement where) {
            return (ScriptException) new ScriptException(stringType,
                    Objects.requireNonNull(context),
                    super.getMessage(),
                    where)
                    .setGenericErrorDescription(genericErrorDescription)
                    .initCause(this);
        }

        /**
         * If "context" is not null, it will return a completed {@link ScriptException} of this Incomplete ScriptException,
         * and if it is null, it will return this.
         */
        public AbstractScriptException completeIfPossible(@Nullable Context context) {
            if (context != null) return complete(context, null);
            else return this;
        }

        public static Incomplete wrapInShouldNotHappen(SQLException exception, String query) {
            return (Incomplete) new Incomplete(ExceptionType.SHOULD_NOT_HAPPEN, "SQL error: " + exception.getMessage() +
                    " (SQL query: " + query + ')').initCause(exception);
        }

        public static Incomplete wrapInShouldNotHappen(Throwable exception) {
            return (Incomplete) new Incomplete(ExceptionType.SHOULD_NOT_HAPPEN, exception.getMessage())
                    .initCause(exception);
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder()
                    .append("Script Error \"")
                    .append(stringType);
            if (super.getMessage() != null)
                sb.append("\": ")
                    .append(super.getMessage());
            sb.append("\nUNABLE TO GET THE SOURCE");
            if (genericErrorDescription != null) {
                sb.append('\n')
                        .append(genericErrorDescription);
            }
            sb.append("\nThe following is the Java stack trace, that might help me as plugin dev if there is a bug.");
            return sb.toString();
        }

        // constructor shortcuts

        public static Incomplete invalidType(String excepted, String got, @Nullable ScriptStackTraceElement where) {
            return new Incomplete(ExceptionType.INVALID_TYPE, "Excepted " + excepted + " but got " + got, where);
        }

        public static Incomplete invalidType(String excepted, ScriptValue<?> got, @Nullable ScriptStackTraceElement where) {
            return invalidType(excepted, Types.getPrettyArgAndType(got), where);
        }

        public static Incomplete invalidType(String excepted, String got) {
            return invalidType(excepted, got, null);
        }

        public static Incomplete invalidType(String excepted, ScriptValue<?> got) {
            return invalidType(excepted, Types.getPrettyArgAndType(got), null);
        }
    }


    // constructor shortcuts

    public static ScriptException invalidType(@NotNull Context context, String excepted, String got, @Nullable ScriptStackTraceElement where) {
        return new ScriptException(ExceptionType.INVALID_TYPE, context, "Excepted " + excepted + " but got " + got, where);
    }

    public static ScriptException invalidType(@NotNull Context context, String excepted, ScriptValue<?> got, @Nullable ScriptStackTraceElement where) {
        return invalidType(context, excepted, Types.getPrettyArgAndType(got), where);
    }

    public static ScriptException invalidType(String excepted, String got, @NotNull ContextStackTraceElement where) {
        return invalidType(where.context, excepted, got, where);
    }

    public static ScriptException invalidType(String excepted, ScriptValue<?> got, @NotNull ContextStackTraceElement where) {
        return invalidType(where.context, excepted, Types.getPrettyArgAndType(got), where);
    }

    /**
     * This just throws a warning in the console you args is not empty
     */
    public static void shouldHaveNoArgs(@NotNull ScriptStackTraceElement where) throws ScriptException {
        if (where.args != null && ! where.args.isEmpty()) {
            CustomLogger.warning("You called the variable " + where + " with arguments (" + where.args + ") but you shouldn't.\n" +
                    "Continuing as if there where no args.");
        }
    }

    public static <O> O requireNonNullElseThrow(O obj, RuntimeException runtimeException) {
        if (obj == null) throw runtimeException;
        return obj;
    }

    public static ScriptException wrapInShouldNotHappen(Throwable exception, @NotNull Context context) {
        return (ScriptException) new ScriptException(ExceptionType.SHOULD_NOT_HAPPEN, context, exception.getMessage())
                .initCause(exception);
    }
}
