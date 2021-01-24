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

package fr.bananasmoothii.scriptcommands.core.configsAndStorage;

import fr.bananasmoothii.scriptcommands.core.antlr4parsing.Parsing;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.PermissionParser;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Command extends ContainingScripts {
    public boolean doNotRegister, confirm;
    public HashMap<String, String> messages;
    public String description;
    public @NotNull String usage;
    public @Nullable PermissionParser.StartContext permissionParseTree;

    public Command(String name, HashMap<String, Object> hashMap) throws IOException, ScriptsParsingException {
        super(name, hashMap, "commands");

        doNotRegister = hashMap.containsKey("do-not-register") && (boolean) hashMap.get("do-not-register");

        if (hashMap.containsKey("confirm")) confirm = (boolean) hashMap.get("confirm");
        else confirm = false;

        messages = (HashMap<String, String>) Config.messages.clone();
        if (hashMap.containsKey("messages")) {
            Config.messages.putAll((Map<String, String>) hashMap.get("messages"));
        }
        description = hashMap.containsKey("description") ? (String) hashMap.get("description") : "Command from ScriptCommand";
        usage = hashMap.containsKey("usage") ? (String) hashMap.get("usage") : "/" + name;
        permissionParseTree = hashMap.containsKey("permission") ? Parsing.parsePermission(name, (String) hashMap.get("permission")) : null;

    }
}
