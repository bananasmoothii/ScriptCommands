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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;
import java.util.*;
import java.util.concurrent.*;

public class ScriptThread implements Future<ScriptValue<?>> {
    private final @NotNull Context context;
    private final @NotNull ParserRuleContext ctxToExecute;
    private final @NotNull String threadName;
    private final @Nullable String group;
    private ScriptsExecutor scriptsExecutor;

    public static final ThreadGroup DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP = new ThreadGroup("ScriptCommand-Threads");
    private static final List<ScriptThread> threads = new ArrayList<>();
    private static final Map<String, Executor> threadGroupExecutors = new HashMap<>();

    /**
     * New instance specifying a group
     * @throws ScriptException if the group does not exist.
     * @see #initialiseThreadGroup(String, Executor)
     */
    public ScriptThread(@NotNull ParserRuleContext ctxToExecute, @NotNull Context context, @Nullable String group) {
        if (! threadGroupExecutors.containsKey(Objects.requireNonNull(group, "group was null in ScriptThread instantiation")))
            throw new ScriptException(ScriptException.ExceptionType.THREAD_GROUP_ERROR, context, "Thread group \"" + group + "\" was not initialised.");
        this.ctxToExecute = Objects.requireNonNull(ctxToExecute, "ctxToExecute was null in ScriptThread instantiation");
        // if it wouldn't be cloned, it would force the user to make separate variables if two thread are doing the same thing...
        this.context = Objects.requireNonNull(context, "context was null in ScriptThread instantiation").clone();
        threadName = getNextThreadName();
        this.group = group;
        threads.add(this);
    }

    /**
     * New instance without a thread group, but with a name. if the name is null, it will be {@link #getNextThreadName()}
     * @throws ScriptException if the name is not available
     */
    public ScriptThread(@Nullable String threadName, @NotNull ParserRuleContext ctxToExecute, @NotNull Context context) {
        if (threadName == null)
            this.threadName = getNextThreadName();
        else if (isAvailableName(threadName)) this.threadName = threadName;
        else
            throw new ScriptException(ScriptException.ExceptionType.THREAD_GROUP_ERROR, context, "Thread name \"" + threadName + "\" is already used.");
        this.ctxToExecute = Objects.requireNonNull(ctxToExecute, "ctxToExecute was null in ScriptThread instantiation");
        // if it wouldn't be cloned, it would force the user to make separate variables if two thread are doing the same thing...
        this.context = Objects.requireNonNull(context, "context was null in ScriptThread instantiation").clone();
        group = null;
        threads.add(this);
    }

    public static boolean isAvailableName(String threadName) {
        if (threadName == null) throw new NullPointerException("threadName is null");
        return threads.stream().noneMatch(sc -> sc.threadName.equals(threadName));
    }

    public ScriptsExecutor getScriptsExecutor() {
        return scriptsExecutor;
    }

    // TODO: add timeout, and call FutureTask#cancel() else
    public void forceReturn() {
        scriptsExecutor.forceReturn();
    }

    public @NotNull String getThreadName() {
        return threadName;
    }

    @Contract(pure = true)
    public static @Nullable ScriptThread getFromName(String threadName) {
        for (ScriptThread thread : threads) {
            if (thread.threadName.equals(threadName)) return thread;
        }
        return null;
    }

    @Contract(pure = true)
    public static @NotNull String getNextThreadName() {
        return "SC-Thread-" + (threads.size() + 1);
    }

    public void start() {
        if (group != null) {
            threadGroupExecutors.get(group).execute(() -> {
                scriptsExecutor = new ScriptsExecutor(context);
                scriptsExecutor.visit(ctxToExecute);
            });
        } else {
            new Thread(DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP, () -> {
                scriptsExecutor = new ScriptsExecutor(context);
                scriptsExecutor.visit(ctxToExecute);
            }).start();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public ScriptValue<?> get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public ScriptValue<?> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    /**
     * What is called a "thread group" is in fact an {@link Executor}. You can create a new thread group here, or
     * modify an existing thread group with {@link #getThreadGroup(String)}. It is advised to set {@link #DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP}
     * as thread group for new threads in your executor.
     * @throws InvalidNameException if the name provided already exists
     */
    public static void initialiseThreadGroup(String name, Executor executor) throws InvalidNameException {
        if (threadGroupExecutors.containsKey(name)) throw new InvalidNameException("That thread group already exists");
        threadGroupExecutors.put(name, executor);
    }

    /**
     * @see #initialiseThreadGroup(String, Executor)
     */
    public static @Nullable Executor getThreadGroup(String name) {
        return threadGroupExecutors.get(name);
    }
}
