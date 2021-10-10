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

package fr.bananasmoothii.scriptcommands.bukkit;

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.Config;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ContainingScripts;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.Storage;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScriptCommands extends JavaPlugin {

    private static ScriptCommands inst;

    @Override
    public void onEnable() {
        inst = this;
        CustomLogger.setLogger(getLogger());
        Config.load(this::saveDefaultConfig);
        Context.registerMethodsFromClass(BukkitUsableFunctions.class);

        Context.threadTrigger(ContainingScripts.Type.EVENT, "server_start", null);

    }

    @Override
    public void onDisable() {
        Storage.saveAndClose();
    }

    public static ScriptCommands inst() {
        return inst;
    }
}

//TODO: make the default commands (reload...)
//TODO: onServerStart and onServerStop events
//TODO: support drag-and-drop files
//TODO: better packages (for example I don't like ScriptValueList being in configsAndStorage and ScriptValue being in execution)
