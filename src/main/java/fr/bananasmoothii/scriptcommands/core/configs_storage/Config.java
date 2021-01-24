package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.ProgressPrinter;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;
import static fr.bananasmoothii.scriptcommands.bukkit.ScriptCommands.logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class Config {
    public static Config firstInstance;

    public String configVersion;
    public Level logLevel;
    public boolean logThroughInfo, directScripts;
    public Storage storage;
    public HashMap<String, String> messages;
    public HashMap<String, Object> rawData;
    public ArrayList<Command> commands;
    public ArrayList<Function> functions;
    public ArrayList<Schedule> schedules;
    public ArrayList<Event> events;

    protected static String missingThing;

    public Config() {
        if (firstInstance == null)
            firstInstance = this;
    }

    /**
     * Not thread-safe
     */
    public static Config load(@NotNull JavaPlugin javaPlugin) throws InvalidConfigException {

        Yaml yamlParser = new Yaml();

        File file = new File("plugins/ScriptCommands/config.yml");
        System.out.println("deleting config file");
        file.delete(); // TODO: remove this line because this is just for testing.
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            logger.info("Config file not found. Creating a new one...");
            javaPlugin.saveDefaultConfig();
            try {
                sc = new Scanner(file);
            } catch (FileNotFoundException e1) {
                logger.severe("Unable to generate a config file:");
                e1.printStackTrace();
                return null;
            }
        }
        ArrayList<String> listLines = new ArrayList<>();
        while (sc.hasNextLine()) {
            listLines.add(sc.nextLine());
        }
        sc.close();

        Config newConfig = new Config();
        try {
            /*
            TODO: try with this when it's working:
                InputStream inputStream = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("customer.yaml");
                    Map<String, Object> obj = yaml.load(inputStream);
             */
            missingThing = "the hole config";
            newConfig.rawData = yamlParser.load(String.join("\n", listLines));

            missingThing = "config-version";
            assert newConfig.rawData.containsKey("config-version");
            newConfig.configVersion = newConfig.rawData.get("config-version").toString();

            missingThing = "log-level";
            newConfig.logLevel = Level.parse(newConfig.rawData.get("log-level").toString());
            assert newConfig.logLevel != null;
            logger.setLevel(newConfig.logLevel);

            missingThing = "log-through-info";
            newConfig.logThroughInfo = (Boolean) newConfig.rawData.get("log-through-info");
            logger.setLogThroughInfo(newConfig.logThroughInfo);
            //assert newConfig.logThroughInfo != null;
            logger.config("test config");

            missingThing = "direct-scripts";
            newConfig.logThroughInfo = (Boolean) newConfig.rawData.get("direct-scripts");

            missingThing = "storage";
            assert newConfig.rawData.containsKey("storage");
            newConfig.storage = new Storage((HashMap<String, ?>) newConfig.rawData.get("storage"));

            missingThing = "messages";
            newConfig.messages = (HashMap<String, String>) newConfig.rawData.get("messages");
            assert newConfig.messages != null;

            missingThing = "messages.bad-args";
            assert newConfig.messages.containsKey("bad-args");
            missingThing = "messages.confirm";
            assert newConfig.messages.containsKey("confirm");
            missingThing = "messages.no-permission";
            assert newConfig.messages.containsKey("no-permission");
            missingThing = "messages.error";
            assert newConfig.messages.containsKey("error");

            newConfig.commands = new ArrayList<>();
            int actualScriptNumber = 0;
            int totalScriptNumber = ((HashMap) newConfig.rawData.get("commands")).size() +
                    ((HashMap) newConfig.rawData.get("functions")).size() +
                    ((ArrayList) newConfig.rawData.get("schedules")).size() +
                    ((HashMap) newConfig.rawData.get("events")).size();
            ProgressPrinter progressPrinter = new ProgressPrinter("Parsing everything: ", 0.5);
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) newConfig.rawData.get("commands")).entrySet()) {
                logger.config("Loading command " + entry.getKey());
                try {
                    newConfig.commands.add(new Command(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    logger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    logger.severe("Errors where encountered while parsing command " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            newConfig.functions = new ArrayList<>();
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) newConfig.rawData.get("functions")).entrySet()) {
                logger.config("Loading function " + entry.getKey());
                try {
                    newConfig.functions.add(new Function(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    logger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    logger.severe("Errors where encountered while parsing function " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            newConfig.schedules = new ArrayList<>();
            for (int i = 0; i < ((ArrayList<HashMap<String, Object>>) newConfig.rawData.get("schedules")).size(); i++) {
                logger.config("Loading schedule N° " + (i + 1));
                //logger.warning();
                try {
                    newConfig.schedules.add(new Schedule("schedule N° " + (i + 1), ((ArrayList<HashMap<String, Object>>) newConfig.rawData.get("schedules")).get(i)));
                } catch (IOException | ScriptsParsingException e1) {
                    logger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    logger.severe("Errors where encountered while parsing schedule " + (i+1) + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            newConfig.events = new ArrayList<>();
            for (Map.Entry<String, HashMap<String, Object>> entry : ((HashMap<String, HashMap<String, Object>>) newConfig.rawData.get("events")).entrySet()) {
                logger.config("Loading event " + entry.getKey());
                try {
                    newConfig.events.add(new Event(entry.getKey(), entry.getValue()));
                } catch (IOException | ScriptsParsingException e1) {
                    logger.severe(e1.getMessage());
                }
                if (ContainingScripts.errorsOnLastParsing)
                    logger.severe("Errors where encountered while parsing event " + entry.getKey() + ". See above for more info.");
                progressPrinter.setProgressPercent((double) ++actualScriptNumber / totalScriptNumber);
            }
            progressPrinter.setFinished();
            return newConfig;
        }
        catch (YAMLException e) {
            throw new InvalidConfigException("The config file is not a valid YAML file. Try to test it with online tools such as http://www.yamllint.com/ . Here is the problem:\n" + e.getMessage() + "\nIf you want to get a fresh new config file, delete or rename the current one.");
        }
        catch (NullPointerException | AssertionError | ClassCastException | IllegalArgumentException e) {
            throw new InvalidConfigException(missingThing + " is missing to the configuration, not valid, or there was an other error.\nError (note: there is always an error): " + e.getClass().getName() + ": " + e.getMessage() );
        }
    }

    /**
     * Used to get the corresponding {@link ContainingScripts}<br/>
     * for getting a schedule, <strong>name</strong> will be {@code "Schedule N° " + (i + 1)}, as schedules start at 1.
     * @return {@code null} if it doesn't exist
     * @throws IllegalArgumentException if <strong>type</strong> is not "commands", "functions", "schedules" or "events".
     */
    @Nullable
    public ContainingScripts getCorrespondingContainingScripts(ContainingScripts.Type type, String name) {
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

    /**
     * to recursively update the config, modifying directly the config file.
     * @param fromVersion the version the config is coming from
     * @return the version it is now
     */
    public static String updateConfig(String fromVersion) {
        // not used yet as the config is the same for all versions.
        return null;
    }
}
