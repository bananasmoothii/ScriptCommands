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

    public ScriptThread(@NotNull Context context, @NotNull ParserRuleContext ctxToExecute) throws InvalidNameException {
        this(context, ctxToExecute, null);
    }

    public ScriptThread(@NotNull Context context, @NotNull ParserRuleContext ctxToExecute, @Nullable ScriptValue<?> id) throws InvalidNameException {
        super(verifyNameDisponibility(id.toString()));
        this.context = context.clone();
        this.ctxToExecute = ctxToExecute;
        this.id = id;
        System.out.println("new ScriptThread !");
    }

    public static String verifyNameDisponibility(String name) throws InvalidNameException {
        if (! isDisponibleName(name))
            throw new InvalidNameException();
        return name;
    }

    public static boolean isDisponibleName(String name) {
        return ! threads.containsKey(name);
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
}
