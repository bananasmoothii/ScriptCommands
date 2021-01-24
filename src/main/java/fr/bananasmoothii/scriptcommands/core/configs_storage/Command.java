package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.antlr4parsing.Parsing;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.PermissionParser;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Command extends ContainingScripts {
    public boolean doNotRegister, confirm;
    public HashMap<String, String> messages;
    public String description;
    public String usage;
    public PermissionParser.StartContext permissionParseTree;

    public Command(String name, HashMap<String, Object> hashMap) throws IOException, ScriptsParsingException {
        super(name, hashMap, "commands");

        doNotRegister = hashMap.containsKey("do-not-register") && (boolean) hashMap.get("do-not-register");

        if (hashMap.containsKey("confirm")) confirm = (boolean) hashMap.get("confirm");
        else confirm = false;

        messages = (HashMap<String, String>) Config.firstInstance.messages.clone();
        if (hashMap.containsKey("messages")) {
            Config.firstInstance.messages.putAll((Map<String, String>) hashMap.get("messages"));
        }
        description = hashMap.containsKey("description") ? (String) hashMap.get("description") : "Command from ScriptCommand";
        usage = hashMap.containsKey("usage") ? (String) hashMap.get("usage") : "/" + name;
        permissionParseTree = hashMap.containsKey("permission") ? Parsing.parsePermission(name, (String) hashMap.get("permission")) : null;

    }
}
