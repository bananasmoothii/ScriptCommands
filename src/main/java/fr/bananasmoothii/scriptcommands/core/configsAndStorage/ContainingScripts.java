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
 * and everything is public.
 * There is one constructor, used to initialize the default fields and have no repeated code
 */
@SuppressWarnings("unchecked")
public abstract class ContainingScripts {
    public final String name;
    public final HashMap<String, Object> hashMap;
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
        else directScripts = Config.directScripts;
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
