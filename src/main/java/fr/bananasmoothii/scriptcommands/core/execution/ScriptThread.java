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

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;
import java.util.HashMap;

public class ScriptThread extends Thread {
    private final @NotNull Context context;
    private final @NotNull ParserRuleContext ctxToExecute;
    private final @Nullable ScriptValue<?> id;
    private static final HashMap<ScriptValue<?>, ScriptThread> threads = new HashMap<>();
    private ScriptsExecutor scriptsExecutor;
    private static int threadsNb = 0;

    public ScriptThread(@NotNull Context context, @NotNull ParserRuleContext ctxToExecute) {
        super();
        this.context = context.clone(); // if it wouldn't be cloned, it would force the user to make separate variables if two thread are doing the same thing...
        this.ctxToExecute = ctxToExecute;
        id = new ScriptValue<>(super.getName());
        threads.put(id, this);
    }

    public ScriptThread(@NotNull Context context, @NotNull ParserRuleContext ctxToExecute, @Nullable ScriptValue<?> id) throws InvalidNameException {
        super(verifyNameAvailability(getValidID(id)).toString());
        this.context = context.clone(); // if it wouldn't be cloned, it would force the user to make separate variables if two thread are doing the same thing...
        this.ctxToExecute = ctxToExecute;
        this.id = id;
        threads.put(id, this);
    }

    public static ScriptValue<?> verifyNameAvailability(ScriptValue<?> id) throws InvalidNameException {
        if (! isAvailableName(id))
            throw new InvalidNameException();
        return id;
    }

    public static boolean isAvailableName(ScriptValue<?> id) {
        return ! threads.containsKey(new ScriptValue<>(id));
    }

    private static @NotNull ScriptValue<?> getValidID(@Nullable ScriptValue<?> id) {
        if (id != null) return id;
        return new ScriptValue<>("ScriptCommands-thread-" + threadsNb++);
    }

    /**
     * You cannot retrieve the result from this as it will be either a function or a block and there is no sense to retrieve the result from it.
     */
    @Override
    public void run() {
        scriptsExecutor = new ScriptsExecutor(context);
        scriptsExecutor.visit(ctxToExecute);
    }

    public ScriptsExecutor getScriptsExecutor() {
        return scriptsExecutor;
    }

    public void forceReturn() {
        scriptsExecutor.forceReturn();
    }

    public ScriptValue<?> getScriptValueId() {
        return id;
    }

    public static ScriptThread getFromId(ScriptValue<?> id) {
        return threads.get(id);
    }
}
