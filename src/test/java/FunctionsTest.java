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

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import fr.bananasmoothii.scriptcommands.core.execution.Args;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;


public class FunctionsTest {

    static {
        CustomLogger.setLevel(Level.FINE);
    }

    @Test
    @Order(1)
    void testArgs() {
        Context context = new Context(null);

        Args.NamingPattern np = new Args.NamingPattern()
                .setNamingPattern(1, "stop")
                .setNamingPattern("start", "stop", "step")
                .setDefaultValue("start", new ScriptValue<>(0))
                .setDefaultValue("step", new ScriptValue<>(1));

        ScriptValueList<Object> list = new ScriptValueList<>();
        list.add(new ScriptValue<>(-1));
        list.add(new ScriptValue<>(5));
        list.add(new ScriptValue<>(0.5));
        list.add(new ScriptValue<>("yay"));

        StringScriptValueMap<Object> map = new StringScriptValueMap<>();
        map.put("somethingOther", new ScriptValue<>("something other value"));
        map.put("step", new ScriptValue<>(0.5));

        Args args = new Args(list, map, context);
        args.setNamingPattern(np);

        CustomLogger.info("start = " + args.getArg("start"));
        assert args.getArg("start").asInteger() == -1;
        CustomLogger.info("stop = " + args.getArg("stop"));
        assert args.getArg("stop").asInteger() == 5;
        CustomLogger.info("step = " + args.getArg("step"));
        assert args.getArg("step").asDouble() == 0.5;
        CustomLogger.info(args.getRemainingArgsList());
        CustomLogger.info(args.getRemainingArgsDictionary());
    }
}
