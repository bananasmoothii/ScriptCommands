package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.antlr4parsing.Parsing;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.ScriptsParser;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A base class for everything that can contain scripts. Fields are:
 * <ul>
 *     <li>{@code String name}</li>
 *     <li>{@code HashMap<String, Object> hashMap}</li>
 *     <li>{@code ArrayList<String> rawScriptLines}</li>
 *     <li>{@code ScriptsParser.StartContext parseTree}</li>
 *     <li>{@code boolean directScripts}</li>
 *     <li>{@code static boolean errorsOnLastParsing}</li>
 * </ul>
 * and everything is public.<br/>
 * There is one constructor, used to initialize the default fields and have no repeated code
 */
public abstract class ContainingScripts {
    public String name;
    public HashMap<String, Object> hashMap;
    public ArrayList<String> rawScriptLines;
    public ScriptsParser.StartContext parseTree;
    public boolean directScripts;
    public static boolean errorsOnLastParsing;

    public ContainingScripts(String name, HashMap<String, Object> hashMap, String nameInConfig) throws IOException, ScriptsParsingException {
        this.name = name;
        this.hashMap = hashMap;
        Config.missingThing = nameInConfig + "." + name + ".script";
        assert hashMap.containsKey("script");
        assert hashMap.get("script") instanceof ArrayList;
        rawScriptLines = (ArrayList<String>) hashMap.get("script");
        if (hashMap.containsKey("direct-scripts")) directScripts = (boolean) hashMap.get("direct-scripts");
        else directScripts = Config.firstInstance.directScripts;
        parseTree = Parsing.reformatAndParse(name, rawScriptLines, directScripts);
        errorsOnLastParsing = parseTree.exception != null;
    }

    public enum Type {
        COMMAND,
        FUNCTION,
        SCHEDULE,
        EVENT
    }
}
