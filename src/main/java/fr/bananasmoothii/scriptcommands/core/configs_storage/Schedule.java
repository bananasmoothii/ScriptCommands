package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;

import java.io.IOException;
import java.util.HashMap;

public class Schedule extends ContainingScripts {
    public int interval, startDelay;

    public Schedule(String name, HashMap<String, Object> hashMap) throws IOException, ScriptsParsingException {
        super(name, hashMap, "schedules");
        Config.missingThing = "schedules." + name + ".interval";
        assert hashMap.containsKey("interval");
        interval = (int) hashMap.get("interval");
        startDelay = hashMap.containsKey("start-delay") ? (int) hashMap.get("start-delay") : 0;

    }
}
