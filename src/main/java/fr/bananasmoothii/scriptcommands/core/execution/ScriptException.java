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
        this(type.getName(), context, message, null);
        genericErrorDescription = type.getDescription();
    }

    /**
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(ExceptionType type, @NotNull Context context, String message, @Nullable ScriptStackTraceElement where) {
        this(type.getName(), context, message, where);
        genericErrorDescription = type.getDescription();
    }

    /**
     * @param where {@link Context} should find it alone if the error was caused by the call of a <strong>function</strong>,
     *                             so {@link Context#callAndRun(String, Args, int, int)} was called
     */
    public ScriptException(ExceptionType type, String message, @NotNull ContextStackTraceElement where) {
        this(type.getName(), where.context, message, where);
        genericErrorDescription = type.getDescription();
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
            this(type.getName(), message, null);
            genericErrorDescription = type.getDescription();
        }

        public Incomplete(ExceptionType type, String message, @Nullable ScriptStackTraceElement where) {
            this(type.getName(), message, where);
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
