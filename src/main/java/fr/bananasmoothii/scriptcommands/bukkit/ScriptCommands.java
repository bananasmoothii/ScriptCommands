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
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.*;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsExecutor;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScriptCommands extends JavaPlugin {

    @Override
    public void onEnable() {
        CustomLogger.setLogger(getLogger());
        Config.load(this::saveDefaultConfig);

        StringScriptValueMap<Object> baseVars = new StringScriptValueMap<>();
        baseVars.put("server", new ScriptValue<>(this.getServer().getName() + getServer().toString()));

        Context.threadTrigger(ContainingScripts.Type.EVENT, "server_start", baseVars);

    }

    @Override
    public void onDisable() {
        Storage.saveAndClose();
    }
}

//TODO: make the default commands (reload...)
//TODO: onServerStart and onServerStop events
