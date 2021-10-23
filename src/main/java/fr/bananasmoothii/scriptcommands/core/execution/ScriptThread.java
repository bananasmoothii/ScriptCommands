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
    private @Nullable Thread thread;
    private @Nullable Future<Void> futureFromExecutorService;
    private boolean isCancelled;

    public static final ThreadGroup DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP = new ThreadGroup("ScriptCommand-Threads");
    private static final List<ScriptThread> threads = new ArrayList<>();
    private static final Map<String, ExecutorService> threadGroupExecutors = new HashMap<>();

    /**
     * New instance specifying a group
     * @param ctxToExecute the ANTLR4 context that will be executed
     * @param context the parent context, will be cloned
     * @param group the thread group name
     * @throws ScriptException if the group does not exist.
     * @see #initialiseThreadGroup(String, ExecutorService)
     */
    public ScriptThread(@NotNull ParserRuleContext ctxToExecute, @NotNull Context context, @NotNull String group) {
        if (! threadGroupExecutors.containsKey(Objects.requireNonNull(group, "group was null in ScriptThread instantiation")))
            throw new ScriptException(ExceptionType.THREAD_GROUP_ERROR, context, "Thread group \"" + group + "\" was not initialised.");
        this.ctxToExecute = Objects.requireNonNull(ctxToExecute, "ctxToExecute was null in ScriptThread instantiation");
        // if it wouldn't be cloned, it would force the user to make separate variables if two thread are doing the same thing...
        this.context = Objects.requireNonNull(context, "context was null in ScriptThread instantiation").clone();
        threadName = getNextThreadName();
        this.group = group;
        threads.add(this);
    }

    /**
     * New instance without a thread group, and with {@link #getNextThreadName()} as name (same as {@link #ScriptThread(String, ParserRuleContext, Context)}
     * with {@code null} as first argument)
     * @param ctxToExecute the ANTLR4 context that will be executed
     * @param context the parent context, will be cloned
     * @throws ScriptException if the name is not available
     */
    public ScriptThread(@NotNull ParserRuleContext ctxToExecute, @NotNull Context context) {
        this(null, ctxToExecute, context);
    }

    /**
     * New instance without a thread group, but with a name. if the name is null, it will be {@link #getNextThreadName()}
     * @param threadName the real name that will be passed to the {@link Thread#Thread(ThreadGroup, Runnable, String)} constructor
     * @param ctxToExecute the ANTLR4 context that will be executed
     * @param context the parent context, will be cloned
     * @throws ScriptException if the name is not available
     */
    public ScriptThread(@Nullable String threadName, @NotNull ParserRuleContext ctxToExecute, @NotNull Context context) {
        if (threadName == null)
            this.threadName = getNextThreadName();
        else if (isAvailableName(threadName)) this.threadName = threadName;
        else
            throw new ScriptException(ExceptionType.UNAVAILABLE_THREAD_NAME, context, "Thread name \"" + threadName + "\" is already used.");
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
        if (isCancelled) return;
        if (group != null) {
            //noinspection unchecked
            futureFromExecutorService = (Future<Void>) threadGroupExecutors.get(group).submit(() -> {
                scriptsExecutor = new ScriptsExecutor(context);
                scriptsExecutor.visit(ctxToExecute);
            });
        } else {
            thread = new Thread(DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP, () -> {
                scriptsExecutor = new ScriptsExecutor(context);
                scriptsExecutor.visit(ctxToExecute);
            }, threadName);
            thread.start();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        isCancelled = true;
        if (mayInterruptIfRunning) {
            if (futureFromExecutorService != null) return futureFromExecutorService.cancel(true);
            if (thread != null) thread.interrupt();
            // if the two above are false it means the thread wasn't run
            return true;
        } else {
            scriptsExecutor.forceReturn();
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return isCancelled || scriptsExecutor.hasReturned();
    }

    @Override
    public @NotNull ScriptValue<?> get() throws InterruptedException, ExecutionException {
        if (isCancelled) return ScriptValue.NONE;
        scriptsExecutor.onReturn(ignored -> ScriptThread.this.notifyAll());
        while (! scriptsExecutor.hasReturned()) {
            wait();
        }
        return scriptsExecutor.getReturned();
    }

    @Override
    public ScriptValue<?> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long callDateNano = System.nanoTime();
        if (isCancelled) return ScriptValue.NONE;
        long timeoutNano = unit.toNanos(timeout);
        long maxDateNano = callDateNano + timeoutNano;
        scriptsExecutor.onReturn(ignored -> ScriptThread.this.notifyAll());
        while (! scriptsExecutor.hasReturned()) {
            if (System.nanoTime() >= maxDateNano) throw new TimeoutException();
            long waitTimeNano = maxDateNano - timeoutNano;
            long waitTimeMilli = TimeUnit.NANOSECONDS.toMillis(waitTimeNano);
            wait(waitTimeMilli, (int) (TimeUnit.MILLISECONDS.toNanos(waitTimeNano) - waitTimeMilli));
        }
        return scriptsExecutor.getReturned();
    }

    /**
     * What is called a "thread group" is in fact an {@link Executor}. You can create a new thread group here, or
     * modify an existing thread group with {@link #getThreadGroup(String)}. It is advised to set {@link #DEFAULT_SCRIPTCOMMANDS_THREAD_GROUP}
     * as thread group for new threads in your executor.
     * @throws InvalidNameException if the name provided already exists
     */
    public static void initialiseThreadGroup(String name, ExecutorService executor) throws InvalidNameException {
        if (threadGroupExecutors.containsKey(name)) throw new InvalidNameException("That thread group already exists");
        threadGroupExecutors.put(name, executor);
    }

    /**
     * @see #initialiseThreadGroup(String, ExecutorService)
     */
    public static @Nullable ExecutorService getThreadGroup(String name) {
        return threadGroupExecutors.get(name);
    }
}
