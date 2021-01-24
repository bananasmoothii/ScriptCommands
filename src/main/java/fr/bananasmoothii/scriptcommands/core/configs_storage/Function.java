package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;

import java.io.IOException;
import java.util.HashMap;

public class Function extends ContainingScripts {

    public Function(String name, HashMap<String, Object> hashMap) throws IOException, ScriptsParsingException {
        super(name, hashMap, "functions");
    }
}
