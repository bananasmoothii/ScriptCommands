package fr.bananasmoothii.scriptcommands.bukkit;

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.configs_storage.Config;
import fr.bananasmoothii.scriptcommands.core.configs_storage.ContainingScripts;
import fr.bananasmoothii.scriptcommands.core.configs_storage.Storage;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class ScriptCommands extends JavaPlugin {

    public static CustomLogger logger;

    @Override
    public void onEnable() {
        logger = new CustomLogger(this.getLogger());
        Config.load(this);

        String command = "commandsThatMakeSense";
        logger.info("Doing command " + command);
        //TODO: make the default commands (reload...)

        ScriptsParser.StartContext context = Config.firstInstance.getCorrespondingContainingScripts(ContainingScripts.Type.COMMAND, command).parseTree;
        HashMap<String, ScriptValue<?>> baseVars = new HashMap<>();
        baseVars.put("player", new ScriptValue<>("Bananasmoothii"));
        baseVars.put("command", new ScriptValue<>(command));
        ScriptsExecutor visitor = new ScriptsExecutor(new Context(new String[] {"arg1", "argu2eul", "argggg3!"}, baseVars));
        visitor.visit(context);

    }

    @Override
    public void onDisable() {
        Storage.firstInstance.saveAndClose();
    }
}
