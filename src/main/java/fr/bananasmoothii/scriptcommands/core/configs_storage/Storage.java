package fr.bananasmoothii.scriptcommands.core.configs_storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;

import static fr.bananasmoothii.scriptcommands.bukkit.ScriptCommands.logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.*;
import java.util.HashMap;

import static fr.bananasmoothii.scriptcommands.core.configs_storage.Config.missingThing;

public class Storage {
    private String method;
    private HashMap<String, ?> hashMap;
    private @Nullable File file;
    private @Nullable Connection connection;
    private @Nullable Statement  statement;

    public static Storage firstInstance;
    public static HashMap<Object, Object> scriptCache = new HashMap<>();

    public Storage(@NotNull HashMap<String, ?> hashMap) {
        firstInstance = this;
        this.hashMap = hashMap;

        missingThing = "storage.method";
        assert hashMap.containsKey("method");
        method = hashMap.get("method").toString();
        assert ! method.equals("method") && hashMap.containsKey(method);

        HashMap<String, Object> methodHashMap;
        switch (method) {
            case "json":
                missingThing = "storage.json";
                methodHashMap = (HashMap<String, Object>) hashMap.get("json");
                assert methodHashMap != null;

                missingThing = "storage.json.file-location";
                assert methodHashMap.containsKey("file-location");
                file = new File((String) methodHashMap.get("file-location"));

                methodHashMap.putIfAbsent("saveAndClose-interval", 1);
                missingThing = "storage.json.saveAndClose-interval";
                assert methodHashMap.get("saveAndClose-interval") instanceof Integer : "saveAndClose-interval must be an integer.";

                break;
            case "SQLite":
                missingThing = "storage.SQLite";
                methodHashMap = (HashMap<String, Object>) hashMap.get("SQLite");
                assert methodHashMap != null;

                missingThing = "storage.SQLite.file-location";
                assert methodHashMap.containsKey("file-location");
                file = new File((String) methodHashMap.get("file-location"));

                methodHashMap.putIfAbsent("table-prefix", "");



                break;
            case "MySQL":
                missingThing = "storage.MySQL";
                methodHashMap = (HashMap<String, Object>) hashMap.get("SQLite");
                assert methodHashMap != null;

                missingThing = "storage.MySQL.hostname";
                assert methodHashMap.containsKey("hostname");

                missingThing = "storage.MySQL.port";
                assert methodHashMap.containsKey("port");

                missingThing = "storage.MySQL.database";
                assert methodHashMap.containsKey("database");

                missingThing = "storage.MySQL.user";
                assert methodHashMap.containsKey("user");

                missingThing = "storage.MySQL.password";
                assert methodHashMap.containsKey("password");

                methodHashMap.putIfAbsent("table-prefix", "");

                methodHashMap.putIfAbsent("flags", "");

                break;
            default:
                throw new IllegalArgumentException("Not a valid storage method: " + method);
        }
    }

    //TODO: make it private again when it will be in the constructor
    HashMap<String, ScriptValue<?>> loadGlobals() { // TODO
        switch (method) {
            case "json":
                if (! file.exists()) {
                    try (FileWriter fileWriter = new FileWriter(file)) {
                        file.createNewFile();
                        fileWriter.write("{\"global_variables\":{}}");
                        fileWriter.flush();
                        logger.fine("Storage file '" + file.getName() + "' was successfully generated.");
                    } catch (IOException e) {
                        logger.severe("Unable to write the json file.");
                        e.printStackTrace();
                    }
                    return new HashMap<>();
                }
                else {
                    try {
                        FileReader reader = new FileReader(file);
                        Gson gson = new Gson();
                        HashMap<String, Object> loaded = gson.fromJson(reader, HashMap.class);
                        System.out.println(loaded);
                        // TODO: make scriptvalues out of it
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        return null;
    }

    public void SQLConnect() throws SQLException { // TODO: remake the SQL things protected when tests are finished
        switch (method) {
            case "SQLite":
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
                    statement = connection.createStatement();
                    System.out.println("Successfully connected to " + file.getName());
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO global_variables VALUES(?, ?, ?);");
                    System.out.println(ps.getClass().getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected storage method: " + method);
        }
    }

    public void executeSQLUpdate(String query) throws SQLException {
        statement.executeUpdate(query);
    }

    public ResultSet executeSQLQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    public PreparedStatement prepareSQLStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public void saveAndClose() {
        try {
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getMethod() {
        return method;
    }

    public HashMap<String, ?> getHashMap() {
        return hashMap;
    }

    public @Nullable File getFile() {
        return file;
    }
}
