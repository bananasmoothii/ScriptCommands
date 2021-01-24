import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.*;
import fr.bananasmoothii.scriptcommands.core.execution.BaseUsableFunctions;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.junit.jupiter.api.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
public class CollectionsTest {

    static {
        File directoryForTests = new File("plugins/ScriptCommands");
        //noinspection ResultOfMethodCallIgnored
        directoryForTests.mkdirs();
    }

    static final String yamlString = "\n" +
            "  # can be \"json\", \"SQLite\" or \"MySQL\"\n" +
            "  method: json" + // TODO: test with json when StringScriptValueMap will be set up, and mySQL
            "\n" +
            "  json:\n" +
            "    file-location: plugins/ScriptCommands/storage.json\n" +
            "    \n" +
            "    # interval in number of modification of any global variable (or any item in a list or dictionary) the plugin will wait for until to saveAndClose everything in the storage file.\n" +
            "    # if your scripts aren't touching global variables a lot, I would recommend letting that on 1 (saveAndClose each 1 modification, so each time) so you won't lose anything if the server crash,\n" +
            "    # but if you're constantly modifying these variables, it can unnecessarily take lots of resources by generating a new file each time.\n" +
            "    save-interval: 5\n" +
            "  SQLite:\n" +
            "    file-location: plugins/ScriptCommands/storage.db\n" +
            "    table-prefix: 'SC_'\n" +
            "  MySQL:\n" +
            "    hostname: ''\n" +
            "    port: 0\n" +
            "    database: ''\n" +
            "    user: ''\n" +
            "    password: ''\n" +
            "    table-prefix: 'SC_'\n" +
            "    #flags: '?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC'";
    static final Yaml yaml = new Yaml();
    static final HashMap<String, Object> storageHashMap = yaml.load(yamlString);
    static { Storage.loadFromHashMap(storageHashMap); }
    static ScriptValueMap<Object, Object> map;
    static ScriptValueList<Object> list;

    @Test
    void main() {
        CustomLogger.info("========= JSON ===========");
        Storage.loadFromHashMap(storageHashMap);
        testEverything();

        CustomLogger.info("\n\n========= SQLITE ===========");
        storageHashMap.put("method", "SQLite");
        Storage.loadFromHashMap(storageHashMap);
        n = 0;
        map = null;
        list = null;
        testEverything();
    }

    void testEverything() {
        retrieveFromGlobals();
        testList();
        testMap();
        contains();
        jsonSerialisationTest();
        globals();
        cloneTest();
        jsonKeysMustBeStrings();
    }

    void testList() {
        list = new ScriptValueList<>(true);

        CustomLogger.info(list.size());

        list.add(new ScriptValue<>(null));
        try {
            list.add(new ScriptValue<>(list));
        } catch (ElementIsCollectionException e) {
            CustomLogger.info(e.getMessage() + " ==> it is working");
        }
        //noinspection SpellCheckingInspection
        list.add(new ScriptValue<>("heyo"));
        list.add(new ScriptValue<>(123456789));

        CustomLogger.info(list.size());

        //list.jsonSave();

        CustomLogger.info(list);
        for (ScriptValue<Object> value: list) {
            CustomLogger.info(value + " " + value.type);
        }

        ScriptValue<Object> b = list.remove(0);
        CustomLogger.info("before: " + b);
        CustomLogger.info(list);

        list.add(1, new ScriptValue<>(true));
        list.set(1, new ScriptValue<>(false));
        CustomLogger.info(list);

        ScriptValueList<Object> subList = list.subList(1, 3);
        CustomLogger.info(subList);

        /*
        long time = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            subList.add(new ScriptValue<>(i));
        }
        subList.toString();
        CustomLogger.info("no sql: " + ((System.nanoTime() - time) * 1E-9d) + "s"); // around 2E-4s

        time = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            list.add(new ScriptValue<>(i));
        }
        list.toString();
        CustomLogger.info("with sql: " + ((System.nanoTime() - time) * 1E-9d) + "s"); // takes 25s for me with SQLite lol, but it's not my main storage disk, Around 500kB/s. It takes 2s on my C disk (~7.5 Mb/s)
        */

        Context.globalVariables.put("someSubList", new ScriptValue<>(subList));
    }

    void contains() {
        ScriptValueList<Object> list = new ScriptValueList<>(true);
        list.add(new ScriptValue<>(true));
        boolean trueObj1 = list.contains(new ScriptValue<>(true));
        CustomLogger.info(trueObj1 + " <== should be true");

        ScriptValueMap<Object, Object> map = new ScriptValueMap<>(true);
        map.put(new ScriptValue<>("some key"), new ScriptValue<>(5555.55));
        boolean trueObj2 = map.containsKey(new ScriptValue<>("some key")),
                trueObj3 = map.containsValue(new ScriptValue<>(5555.55)),
                trueObj4 = ! map.containsKey(new ScriptValue<>(563));
        CustomLogger.info(trueObj2 + " " + trueObj3 + " " + trueObj4 + " <== should all be true");

        assert trueObj1 && trueObj2 && trueObj3 && trueObj4 : "something bugged";
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    void testMap() {
        map = new ScriptValueMap<>(true);
        map.put(new ScriptValue<>("myKey"), new ScriptValue<>(0));

        CustomLogger.info(map);
        CustomLogger.info(map.containsKey(new ScriptValue<>("myKey")));
        map.remove(new ScriptValue<>("myKey"));
        CustomLogger.info(map);

        map.put(new ScriptValue<>("some key"), new ScriptValue<>("some value 1"));
        map.put(new ScriptValue<>("some key"), new ScriptValue<>("some value 2"));
        CustomLogger.info(map);
        CustomLogger.info(map.get(new ScriptValue<>("some key")));

        for (int i = 0; i < 10; i++) {
            map.put(getAScriptValue(), getAScriptValue());
        }
        map.put(new ScriptValue<>(null), getAScriptValue());

        for (Map.Entry<ScriptValue<Object>, ScriptValue<Object>> entry: map.entrySet()) {
            CustomLogger.info("map -> " + entry.getKey() + " = " + entry.getValue());
        }

        CustomLogger.info(map.get(new ScriptValue<>("bllllllllob")));
        CustomLogger.info(map.toString());

        System.out.println("\n\"bllllllllob\" = " + map.get(new ScriptValue<>(12345)));
    }

    private int n = 0;

    /**
     * No randomness is used here because it could validate the test when not testing a critatical value
     */
    @SuppressWarnings("SpellCheckingInspection")
    ScriptValue<Object> getAScriptValue() {
        switch (++n) {
            case 1:
                return new ScriptValue<>(12345);
            case 2:
                return new ScriptValue<>(987654321);
            case 3:
                return new ScriptValue<>(12.3456789);
            case 4:
                return new ScriptValue<>(true);
            case 5:
                return new ScriptValue<>(false);
            case 6:
                return new ScriptValue<>("abcdefg");
            case 7:
                return new ScriptValue<>("bllllllllob");
            //now it becomes toff: lists and dictionaries
            case 8:
                ScriptValueList<Object> list1 = new ScriptValueList<>();
                for (int i = 0; i < n-4; i++) {
                    list1.add(getAScriptValue());
                }
                return new ScriptValue<>(list1);
            case 9:
                ScriptValueMap<Object, Object> dict1 = new ScriptValueMap<>();
                for (int i = 0; i < n-4; i++) {
                    dict1.put(getAScriptValue(), getAScriptValue());
                }
                return new ScriptValue<>(dict1);
            case 10:
                return new ScriptValue<>(100.0);
            default:
                n = 0;
                return new ScriptValue<>(null);
        }
    }

    void globals() {
        ScriptValue<Object> some_int_var_in_scripts = Context.globalVariables.get("some_int_var_in_scripts");
        if (some_int_var_in_scripts == null)
            Context.globalVariables.put("some_int_var_in_scripts", new ScriptValue<>(0));
        else
            Context.globalVariables.put("some_int_var_in_scripts", new ScriptValue<>((some_int_var_in_scripts.asInteger() + 1)));
        Context.globalVariables.put("otherVarTest", new ScriptValue<>(new ScriptValueList<>()));
        Context.globalVariables.put("someList", new ScriptValue<>(list));
        Context.globalVariables.put("someMap", new ScriptValue<>(map));
        CustomLogger.info("globals in globals(): " + Context.globalVariables + Context.globalVariables.getSQLTable());
    }


    void cloneTest() {
        Storage.jsonSave();
        StringScriptValueMap<Object> clone = Context.globalVariables.clone();
        StringScriptValueMap<Object> theGlobal = StringScriptValueMap.getTheGlobal(true);
        Storage.jsonSave();
        CustomLogger.info(clone);
        CustomLogger.info("should equal");
        CustomLogger.info(theGlobal);
        assert clone.equals(theGlobal);
    }


    void jsonKeysMustBeStrings() {
        StringScriptValueMap<Object> clone = Context.globalVariables.clone();
        CustomLogger.info("normal classes for json:");
        HashMap<String, Object> normals = Context.globalVariables.toNormalClasses(true);
        for (Map.Entry<String, Object> entry: normals.entrySet()) {
            if (entry.getValue() instanceof HashMap) {
                CustomLogger.info(entry.getKey() + " = ");
                for (Map.Entry<?, ?> entry2: ((HashMap<?, ?>) entry.getValue()).entrySet()) {
                    CustomLogger.info("| " + entry2.getKey() + " = " + entry2.getValue() + " - key: " + entry2.getKey().getClass().getName());
                    assert entry2.getKey() instanceof String : "Key is not savable in json, it must be String and it is " + entry2.getKey().getClass().getName();
                }
            } else {
                CustomLogger.info(entry.getKey() + " = " + entry.getValue());
            }
        }
        Storage.jsonSave();
        CustomLogger.info("the clone from before: " + clone);
        CustomLogger.info("the globals now: " + StringScriptValueMap.getTheGlobal(true));
        assert StringScriptValueMap.getTheGlobal().equals(clone);
        CustomLogger.info("the clone from before now: " + clone);
    }

    void retrieveFromGlobals() {
        boolean fileExisted = Storage.getFile().exists();
        CustomLogger.info("retrieved globals: " + StringScriptValueMap.getTheGlobal().toString());
        //assert ! fileExisted || StringScriptValueMap.getTheGlobal().size() > 0;
    }


    void jsonSerialisationTest() {
        ScriptValueMap<Object, Object> map1 = new ScriptValueMap<>();
        map1.put(new ScriptValue<>(100.0), new ScriptValue<>(null));

        ScriptValueList<Object> list1 = new ScriptValueList<>();
        list1.add(new ScriptValue<>(map1));

        CustomLogger.info(list1);
    }

    @AfterAll
    static void finish() {
        Storage.saveAndClose();
    }
}
