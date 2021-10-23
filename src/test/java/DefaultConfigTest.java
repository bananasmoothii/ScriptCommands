/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.Config;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ContainingScripts;
import fr.bananasmoothii.scriptcommands.core.execution.Args;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.NoneType;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import fr.bananasmoothii.scriptcommands.core.functions.ScriptFunctionMethod;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static fr.bananasmoothii.scriptcommands.core.execution.ScriptValue.NONE;

public class DefaultConfigTest {

    @Test
    void main() {
        File configInPlugins = new File("plugins/ScriptCommands/config.yml");
        configInPlugins.delete();
        Config.load(() -> {
            try {
                Files.copy(Paths.get("src/test/resources/config.yml"), configInPlugins.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Context.registerMethodsFromClass(DummyBukkitUsableFunctions.class);
        Context.threadTrigger(ContainingScripts.Type.FUNCTION, "test", null);
    }

    static class DummyBukkitUsableFunctions {

        @ScriptFunctionMethod
        public static ScriptValue<NoneType> console_cmd(Args args) {
            CustomLogger.info("COMMAND: " + args.getSingleArg());
            return NONE;
        }
    }
}
