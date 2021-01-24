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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.execution.BaseUsableFunctions;

import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.bananasmoothii.scriptcommands.core.configsAndStorage.Config.missingThing;

@SuppressWarnings("unchecked")
public abstract class Storage {
    private static StorageMethod method = StorageMethod.JSON;
    private static @NotNull String sqlTablePrefix = "SC_";
    public static boolean isSQL = false;
    private static HashMap<String, ?> hashMap;
    private static  @Nullable File file = new File("storage.json");
    public static final Gson gson;
    static {
        JsonDeserializerFix adapter = new JsonDeserializerFix();
        gson = new GsonBuilder()
                .registerTypeAdapter(Map.class, adapter)
                .registerTypeAdapter(List.class, adapter)
                .serializeNulls()
                .create();
    }
    private static int jsonSaveInterval = 5;
    private static  @Nullable Connection connection;

    public static void loadFromHashMap(@NotNull HashMap<String, ?> hashMap) {
        Storage.hashMap = hashMap;

        missingThing = "storage.method";
        assert hashMap.containsKey("method");
        String sMethod = hashMap.get("method").toString();
        assert ! sMethod.equals("method") && hashMap.containsKey(sMethod);

        sqlTablePrefix = (String) ((HashMap<String, Object>) hashMap.get(sMethod)).get("table-prefix");
        isSQL = sMethod.contains("SQL");

        HashMap<String, Object> methodHashMap;
        switch (sMethod) {
            case "json":
                method = StorageMethod.JSON;

                missingThing = "storage.json";
                methodHashMap = (HashMap<String, Object>) hashMap.get("json");
                assert methodHashMap != null;

                missingThing = "storage.json.file-location";
                assert methodHashMap.containsKey("file-location");
                file = new File((String) methodHashMap.get("file-location"));

                methodHashMap.putIfAbsent("save-interval", 1);
                missingThing = "storage.json.save-interval";
                assert methodHashMap.get("save-interval") instanceof Integer : "save-interval must be an integer.";
                jsonSaveInterval = (int) methodHashMap.get("save-interval");

                break;
            case "SQLite":
                method = StorageMethod.SQLITE;

                missingThing = "storage.SQLite";
                methodHashMap = (HashMap<String, Object>) hashMap.get("SQLite");
                assert methodHashMap != null;

                missingThing = "storage.SQLite.file-location";
                assert methodHashMap.containsKey("file-location");
                file = new File((String) methodHashMap.get("file-location"));

                methodHashMap.putIfAbsent("table-prefix", "");

                try {
                    sqlConnect();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                break;
            case "MySQL":
                method = StorageMethod.MYSQL;

                missingThing = "storage.MySQL";
                methodHashMap = (HashMap<String, Object>) hashMap.get("MySQL");
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

                methodHashMap.putIfAbsent("flags", "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");

                try {
                    sqlConnect();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                break;
            default:
                throw new IllegalArgumentException("Not a valid storage method: " + sMethod);
        }

        Context.globalVariables = StringScriptValueMap.getTheGlobal(true);
    }

    public enum StorageMethod {
        JSON,
        SQLITE,
        MYSQL
    }

    protected static void sqlConnect() throws SQLException {
        switch (method) {
            case SQLITE:
                try {
                    Class.forName("org.sqlite.JDBC");
                    //noinspection ConstantConditions // because if the method is SQLite, file won't be null
                    connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
                    CustomLogger.config("Successfully connected to " + file.getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case MYSQL:
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    HashMap<String, ?> mySQLHashMap = (HashMap<String, ?>) hashMap.get("MySQL");
                    String hostname = (String) mySQLHashMap.get("hostname");
                    int port = (int) (Integer) mySQLHashMap.get("port");
                    String database = (String) mySQLHashMap.get("database");
                    String user = (String) mySQLHashMap.get("user");
                    String password = (String) mySQLHashMap.get("password");
                    String flags = (String) mySQLHashMap.get("flags") ;
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ':' + port + '/' + database + flags, user, password);
                    CustomLogger.config("Successfully connected to the MySQl webserver");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalArgumentException("Not a valid storage method for SQL: " + method);
        }
    }

    protected static void executeSQLUpdate(String query) throws SQLException {
        if (connection == null) throw new NotUsingSQLException();
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
    }

    protected static ResultSet executeSQLQuery(String query) throws SQLException {
        if (connection == null) throw new NotUsingSQLException();
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    protected static PreparedStatement prepareSQLStatement(String sql) throws SQLException {
        if (connection == null) throw new NotUsingSQLException();
        return connection.prepareStatement(sql);
    }

    protected static boolean sqlTableExists(String sqlTable) {
        if (connection == null) throw new NotUsingSQLException();
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, sqlTable, null);
            return tables.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cleans the database by removing collections that have no reference, so not accessible from
     * global variables.
     */
    public static void cleanDatabase() {
        if (connection == null) throw new NotUsingSQLException();
        //TODO
    }

    public static void jsonSave() {
        if (method != StorageMethod.JSON) return;
        try {
            //noinspection ResultOfMethodCallIgnored,ConstantConditions // because if the method is json, file won't be null
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            HashMap<String, Object> holeJsonHashMap = new HashMap<>();
            //long start = System.nanoTime();
            if (Context.globalVariables != null)
                holeJsonHashMap.put("global_vars", Context.globalVariables.toNormalClasses(true));
            else {
                holeJsonHashMap.put("global_vars", new HashMap<>());
                CustomLogger.warning("Could not save global variables with json because BaseUsableFunction didn't got initialized with a json Storage. " +
                        "This is probably because no script was run.");
            }
            if (gson != null) gson.toJson(holeJsonHashMap, fileWriter);
            else throw new NullPointerException("This Storage class is not using JSON");
            //CustomLogger.info("Gson with 'keys for json' took " + (System.nanoTime() - start) + "ns");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAndClose() {
        if (isSQL) {
            try {
                //noinspection ConstantConditions
                connection.close();
            } catch (SQLException | NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            jsonSave();
        }
    }

    public static StorageMethod getMethod() {
        return method;
    }

    public static void setMethod(StorageMethod method) {
        Storage.method = method;
    }

    /**
     * @return The prefix of the tables in SQL, {@code null} if SQL isn't used.
     */
    public static @Nullable String getSQLTablePrefix() {
        return sqlTablePrefix;
    }

    public static HashMap<String, ?> getHashMap() {
        return hashMap;
    }

    public static void setHashMap(HashMap<String, ?> hashMap) {
        Storage.hashMap = hashMap;
    }

    public static @Nullable File getFile() {
        return file;
    }

    public static void setFile(@Nullable File file) {
        Storage.file = file;
    }

    public static int getJsonSaveInterval() {
        return jsonSaveInterval;
    }

    public static void setJsonSaveInterval(int jsonSaveInterval) {
        Storage.jsonSaveInterval = jsonSaveInterval;
    }
}
