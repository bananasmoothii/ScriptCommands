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
