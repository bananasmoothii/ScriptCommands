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

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.ProgressPrinter;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public abstract class Config {
    public static String configVersion;
    public static UpdateMethod update;
    public static Level logLevel;
    public static boolean logThroughInfo, directScripts;
    public static HashMap<String, String> messages;
    public static HashMap<String, Object> rawData;
    public static ArrayList<Command> commands;
    public static ArrayList<Function> functions;
    public static ArrayList<Schedule> schedules;
    public static ArrayList<Event> events;

    protected static String missingThing;

    /**
     * Not thread-safe
     * @param createConfig a {@link Runnable} that will be used in case if the config was not found.
     */
    public static void load(@NotNull Runnable createConfig) throws InvalidConfigException {

        Yaml yamlParser = new Yaml();

        File file = new File("plugins/ScriptCommands/config.yml");

        CustomLogger.config("deleting config file");
        //noinspection ResultOfMethodCallIgnored
        file.delete(); // TODO: remove this line because this is just for testing.

        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            CustomLogger.info("Config file not found. Creating a new one...");
            createConfig.run();
            try {
                sc = new Scanner(file);
            } catch (FileNotFoundException e1) {
                CustomLogger.severe("Unable to generate a config file:");
                e1.printStackTrace();
                return;
            }
        }
        ArrayList<String> listLines = new ArrayList<>();
        while (sc.hasNextLine()) {
            listLines.add(sc.nextLine());
        }
        sc.close();
        try {
            /*
            TODO: try with this when it's working:
                InputStream inputStream = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("customer.yaml");
                    Map<String, Object> obj = yaml.load(inputStream);
             */
            missingThing = "a valid config";
            rawData = yamlParser.load(String.join("\n", listLines));

            missingThing = "config-version";
            assert rawData.containsKey("config-version");
            configVersion = rawData.get("config-version").toString();

            missingThing = "update";
            assert rawData.containsKey("update");
            update = UpdateMethod.valueOf(rawData.get("update").toString().toUpperCase());

            missingThing = "log-level";
            logLevel = Level.parse(rawData.get("log-level").toString());
            assert logLevel != null;
            CustomLogger.setLevel(logLevel);

            missingThing = "log-through-info";
            logThroughInfo = (Boolean) rawData.get("log-through-info");
            CustomLogger.setLogThroughInfo(logThroughInfo);
            //assert newConfig.logThroughInfo != null;
            CustomLogger.config("test config");

            missingThing = "direct-scripts";
            logThroughInfo = (Boolean) rawData.get("direct-scripts");

            missingThing = "storage";
            assert rawData.containsKey("storage");
            Storage.loadFromHashMap((HashMap<String, ?>) rawData.get("storage"));

            missingThing = "messages";
            messages = (HashMap<String, String>) rawData.get("messages");
            assert messages != null;

            missingThing = "messages.bad-args";
            assert messages.containsKey("bad-args");
            missingThing = "messages.confirm";
            assert messages.containsKey("confirm");
            missingThing = "messages.no-permission";
            assert messages.containsKey("no-permission");
            missingThing = "messages.error";
            assert messages.containsKey("error");

            commands = new ArrayList<>();
            int actualScriptNumber = 0;
            @SuppressWarnings("rawtypes")
            int totalScriptNumber = ((HashMap) rawData.get("commands")).size() +
                    ((HashMap) rawData.get("functions")).size() +
                    ((ArrayList) rawData.get("schedules")).size() +
                    ((HashMap) rawData.get("events")).size();
            ProgressPrinter progressPrinter = new ProgressPrinter("Parsing everything: ", 0.5);
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) rawData.get("commands")).entrySet()) {
                CustomLogger.config("Loading command " + entry.getKey());
                try {
                    commands.add(new Command(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    CustomLogger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    CustomLogger.severe("Errors where encountered while parsing command " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            functions = new ArrayList<>();
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) rawData.get("functions")).entrySet()) {
                CustomLogger.config("Loading function " + entry.getKey());
                try {
                    functions.add(new Function(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    CustomLogger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    CustomLogger.severe("Errors where encountered while parsing function " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            schedules = new ArrayList<>();
            for (int i = 0; i < ((ArrayList<HashMap<String, Object>>) rawData.get("schedules")).size(); i++) {
                CustomLogger.config("Loading schedule N° " + (i + 1));
                try {
                    schedules.add(new Schedule("schedule N° " + (i + 1), ((ArrayList<HashMap<String, Object>>) rawData.get("schedules")).get(i)));
                } catch (IOException | ScriptsParsingException e1) {
                    CustomLogger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    CustomLogger.severe("Errors where encountered while parsing schedule " + (i+1) + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            events = new ArrayList<>();
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) rawData.get("events")).entrySet()) {
                CustomLogger.config("Loading event " + entry.getKey());
                try {
                    events.add(new Event(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    CustomLogger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    CustomLogger.severe("Errors where encountered while parsing event " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            progressPrinter.setFinished();
        }
        catch (YAMLException e) {
            throw new InvalidConfigException("The config file is not a valid YAML file. Try to test it with online tools such as http://www.yamllint.com/ . Here is the problem:\n"
                    + e.getMessage() + "\nIf you want to get a fresh new config file, delete or rename the current one.");
        }
        catch (NullPointerException | AssertionError | ClassCastException | IllegalArgumentException e) {
            throw new InvalidConfigException(missingThing + " is missing to the configuration, not valid, or there was an other error.\nError (note: there is always an error): "
                    + e.getClass().getName() + ": " + e.getMessage() );
        }
    }

    /**
     * Used to get the corresponding {@link ContainingScripts}<br/>
     * for getting a schedule, <strong>name</strong> will be {@code "Schedule N° " + (i + 1)}, as schedules start at 1.
     * @return {@code null} if it doesn't exist
     * @throws IllegalArgumentException if <strong>type</strong> is not "commands", "functions", "schedules" or "events".
     */
    @Nullable
    public static ContainingScripts getCorrespondingContainingScripts(ContainingScripts.Type type, String name) {
        switch (type) {
            case COMMAND:
                for (Command command : commands) {
                    if (command.name.equals(name)) return command;
                }
                return null;
            case FUNCTION:
                for (Function function : functions) {
                    if (function.name.equals(name)) return function;
                }
                return null;
            case SCHEDULE:
                for (Schedule schedule : schedules) {
                    if (schedule.name.equals(name)) return schedule;
                }
                return null;
            case EVENT:
                for (Event event : events) {
                    if (event.name.equals(name)) return event;
                }
                return null;
            default:
                throw new IllegalArgumentException("invalid type");
        }
    }

    /*
     * to recursively update the config, modifying directly the config file.
     * @param fromVersion the version the config is coming from
     * @return the version it is now
     *
    @SuppressWarnings("SameReturnValue")
    public static String updateConfig(String fromVersion) {
        // not used yet as the config is the same for all versions.
        return null;
    }*/

    public enum UpdateMethod {
        AUTO,
        ASK,
        NO
    }
}
