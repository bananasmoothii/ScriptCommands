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
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Similar to {@link ScriptValueMap} but for global variables, with names. <br>
 * This can store values in normal SQL tables just like {@link ScriptValueMap} because the code was just copy/pasted,
 * but this would be no use...
 * @param <V> the type of {@link ScriptValue} this map will store as values
 */
@SuppressWarnings({"unchecked", "ConstantConditions"})
public class StringScriptValueMap<V> extends AbstractMap<String, ScriptValue<V>> implements ScriptValueCollection {

    /** with prefix, null if isSQL is false. */
    private @Nullable String SQLTable;

    /** @see ScriptValueCollection#getSQLTable() */
    public static final char typeChar = 's';

    private boolean isTheGlobal;

    private static StringScriptValueMap<Object> theGlobal;

    private int timesModifiedSinceLastSave = 1;

    /**
     * Not null only with json, use {@code hashMap == null} to know if this map (class) is using SQL.
     */
    private @Nullable HashMap<String, ScriptValue<V>> hashMap;

    private @NotNull StringID stringID;

    private boolean useSQLIfPossible;

    private static StringID lastStringID;

    public StringScriptValueMap() {
        this(false, false);
    }

    public StringScriptValueMap(boolean useSQLIfPossible) {
        this(useSQLIfPossible, false);
    }

    /**
     * @param useSQLIfPossible if false, everything will be stored in a normal {@link HashMap}
     * @param isTheGlobal, if true, this will be the global StringScriptValueMap.
     * @see #getTheGlobal()
     */
    public StringScriptValueMap(boolean useSQLIfPossible, boolean isTheGlobal) {
        super();
        this.useSQLIfPossible = useSQLIfPossible;
        // from here to the creation of the SQL table, "this" will throw an error in debuggers because the table doesn't exist, so size() or toString() won't work
        if (isTheGlobal && theGlobal != null)
            throw new IllegalStateException("a global StringScriptValueMap was already defined");
        this.isTheGlobal = isTheGlobal;
        if (isTheGlobal)
            theGlobal = (StringScriptValueMap<Object>) this;
        stringID = getNewStringID();
        if (canUseSQL()) {
            SQLTable = getFullSQLTableName();
            boolean tableWasCreated = ! Storage.sqlTableExists(SQLTable);
            String query = "CREATE TABLE " + (isTheGlobal ? "IF NOT EXISTS " : "") + '`' + SQLTable + "` (`name` VARCHAR(100) PRIMARY KEY, `value_object` TEXT, `value_type` TINYINT NOT NULL)";
            try {
                Storage.executeSQLUpdate(query);
                if (isTheGlobal && tableWasCreated) {
                    query = "CREATE UNIQUE INDEX `PK_global_vars` ON `" + SQLTable + "` (`name`)";
                    Storage.executeSQLUpdate(query);
                }
            } catch (SQLException e) {
                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
            }
        } else {
            hashMap = new HashMap<>();
        }
        modified();
    }

    public StringScriptValueMap(Map<String, ScriptValue<V>> map) {
        this();
        putAll(map);
    }

    // the two following constructors won't be used

    public StringScriptValueMap(@NotNull String SQLTable) {
        this(
                new StringID(
                        SQLTable.substring(
                                ScriptException.requireNonNullElseThrow(
                                        Storage.getSQLTablePrefix(),
                                        new NotUsingSQLException("The prefix of the given Storage class is null, probably because it isn't using SQL.")
                                ).length()
                        )
                )
        );
    }

    /**
     * Constructs a new ScriptValueList using an existing SQL table, found with the {@link StringID}
     */
    public StringScriptValueMap(@NotNull StringID stringID) {
        super();
        if (! Storage.isSQL)
            throw new NotUsingSQLException("the provided Storage class is not using SQL");
        this.stringID = stringID;
        SQLTable = getFullSQLTableName();
        if (! Storage.sqlTableExists(SQLTable))
            throw new NullPointerException("SQL table " + SQLTable + " does not exist");
        modified();
    }

    private static StringID getNewStringID() {
        StringID currentID = lastStringID != null ? lastStringID.nextID() : new StringID(0);
        if (Storage.isSQL) {
            while (Storage.sqlTableExists(getFullSQLTableName(currentID, false))) {
                currentID = currentID.nextID();
            }
        }
        lastStringID = currentID;
        return currentID;
    }

    /**
     * @see #getTheGlobal(boolean)
     */
    public static StringScriptValueMap<Object> getTheGlobal() {
        return getTheGlobal(false);
    }

    /**
     * @param hardReload if true, it will not use an instance that already exist: {@link #theGlobal}
     */
    public static StringScriptValueMap<Object> getTheGlobal(boolean hardReload) {
        if (!hardReload && theGlobal != null) return theGlobal;
        switch (Storage.getMethod()) {
            case JSON:
                File file = Storage.getFile();
                //noinspection ConstantConditions // because if the method is json, file won't be null
                if (! file.exists()) {
                    regenerateJson();
                    new StringScriptValueMap<>(true, true);
                }
                else {
                    try (FileReader reader = new FileReader(file)) {
                        Map<String, Object> loadedGlobals;
                        try {
                            loadedGlobals = (Map<String, Object>) Storage.gson.fromJson(reader, Map.class).get("global_vars");
                        } catch (NullPointerException e) {
                            regenerateJson();
                            loadedGlobals = new HashMap<>();
                        }
                        if (theGlobal == null)
                            new StringScriptValueMap<>(true, true);
                        theGlobal.timesModifiedSinceLastSave = -loadedGlobals.size() - 1; // for not making it save because it will save the same
                        theGlobal.clear();
                        theGlobal.putAll((StringScriptValueMap<Object>) StringScriptValueMap.toScriptValues(loadedGlobals, true));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MYSQL:
            case SQLITE:
                if (hardReload)
                    theGlobal = null;
                if (theGlobal == null)
                    new StringScriptValueMap<>(true, true);
        }
        return theGlobal;
    }

    public static void regenerateJson() {
        if (Storage.getMethod() != Storage.StorageMethod.JSON) throw new RuntimeException("Storage is not using Json");
        File file = Storage.getFile();
        try (FileWriter fileWriter = new FileWriter(file)) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            fileWriter.write("{\"global_vars\":{}}");
            fileWriter.flush();
            CustomLogger.fine("Storage file '" + file.getName() + "' was successfully (re)generated.");
        } catch (IOException e) {
            CustomLogger.severe("Unable to write the json file.");
            e.printStackTrace();
        }
    }

    private int lastSize = -1;

    @Override
    @Contract(pure = true)
    public int size() {
        if (hashMap != null) return hashMap.size();
        if (lastSize != -1 && timesModifiedSinceLastSave == 0) return lastSize;
        String query = "SELECT COUNT(*) FROM `" + SQLTable + '`';
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            rs.next();
            lastSize = rs.getInt(1);
            return lastSize;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (! (value instanceof ScriptValue)) return false;
        if (hashMap != null) return hashMap.containsValue(value);
        String query = "SELECT `value_object`, `value_type` FROM `" + SQLTable + '`';
        ScriptValue<?> value1 = (ScriptValue<?>) value;
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            while (rs.next()) {
                byte type = rs.getByte(2);
                if (type != value1.type.asByte) continue;
                ScriptValue<?> objectTested = ScriptValueCollection.transformToScriptValue(rs.getString(1), type);
                if (objectTested.equals(value1)) return true;
            }
            return false;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (! (key instanceof String)) return false;
        if (hashMap != null) return hashMap.containsKey(key);
        String query = "SELECT 1 FROM `" + SQLTable + "` WHERE `name` = ?";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setString(1, (String) key);
            query += " => " + ps;
            return ps.executeQuery().next();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    /**
     * If this returns {@code null}, it means there was an error or your key wasn't a {@link ScriptValue},
     * because if this works, it should return a
     * <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code>
     */
    @Override
    public ScriptValue<V> get(Object key) {
        if (! (key instanceof String)) return null;
        if (hashMap != null) return hashMap.get(key);
        String query = "SELECT `value_object`, `value_type` FROM `" + SQLTable + "` WHERE `name` = ?";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setString(1, (String) key);
            query += " => " + ps;
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return (ScriptValue<V>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2));
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @Override
    public ScriptValue<V> put(String key, ScriptValue<V> value) {
        if (key.length() > 100) throw new IllegalArgumentException("the key in longer than 100 characters");
        ScriptValue<V> previousElement;
        if (hashMap != null) previousElement = hashMap.put(key, value);
        else {
            previousElement = get(key);
            if (containsKey(key)) {
                String query = "UPDATE `" + SQLTable + "` SET `value_object` = ?, `value_type` = ? WHERE `name` = ?";
                try {
                    PreparedStatement ps = Storage.prepareSQLStatement(query);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, value, 1, 2);
                    ps.setString(3, key);
                    query += " => " + ps;
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                }
            } else {
                String query = "INSERT INTO `" + SQLTable + "` VALUES(?, ?, ?)";
                try {
                    PreparedStatement ps = Storage.prepareSQLStatement(query);
                    ps.setString(1, key);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, value, 2, 3);
                    query += " => " + ps;
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                }
            }
        }
        modified();
        return previousElement;
    }

    @Override
    public ScriptValue<V> remove(Object key) {
        ScriptValue<V> previousElement;
        if (hashMap != null) previousElement = hashMap.remove(key);
        else {
            previousElement = get(key);
            String query = "DELETE FROM `" + SQLTable + "` WHERE `name` = ?";
            try {
                PreparedStatement ps = Storage.prepareSQLStatement(query);
                ps.setString(1, (String) key);
                query += " => " + ps;
                ps.executeUpdate();
            } catch (SQLException e) {
                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
            }
        }
        modified();
        return previousElement;
    }

    @Override
    public void clear() {
        ArrayList<String> keys = new ArrayList<>(keySet());
        for (String s: keys) {
            remove(s);
        }
        modified();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        if (hashMap != null) return hashMap.keySet();
        String query = "SELECT `name` FROM `" + SQLTable + '`';
        try {
            ResultSet rs1 = Storage.executeSQLQuery(query);
            int size1 = size();
            return new AbstractSet<String>() {
                final int size = size1;
                final ResultSet rs = rs1;

                @Override
                public @NotNull Iterator<String> iterator() {
                    return new Iterator<String>() {
                        @Override
                        public boolean hasNext() {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }
                        }

                        @Override
                        public String next() {
                            try {
                                return rs.getString(1);
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }
                        }
                    };
                }

                @Override
                public int size() {
                    return size;
                }
            };
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @NotNull
    @Override
    public ScriptValueList<V> values() {
        if (hashMap != null) new ScriptValueList<>(hashMap.values());
        String query = "SELECT `value_object`, `value_type` FROM `" + SQLTable + '`';
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            ScriptValueList<V> list = new ScriptValueList<>();
            while (rs.next()) {
                list.add((ScriptValue<V>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2)));
            }
            return list;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @NotNull
    @Override
    public Set<Entry<String, ScriptValue<V>>> entrySet() {
        if (hashMap != null) return hashMap.entrySet();
        String query = "SELECT * FROM `" + SQLTable + '`';
        try {
            ResultSet rs1 = Storage.executeSQLQuery(query);
            int size1 = size();
            // sry for debugging, with all these nested lambdas xD
            return new AbstractSet<Entry<String, ScriptValue<V>>>() {
                final int size = size1;
                final ResultSet rs = rs1;

                @Override
                public int size() {
                    return size;
                }

                @Override
                public @NotNull Iterator<Entry<String, ScriptValue<V>>> iterator() {
                    return new Iterator<Entry<String, ScriptValue<V>>>() {
                        @Override
                        public boolean hasNext() {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }
                        }

                        @Override
                        public Entry<String, ScriptValue<V>> next() {
                            /*
                             For some cloudy reason, when you do the "try return ResultSet.get... catch" block inside
                             the getKey and getValue methods, the debugger can't look at what's inside of the map,
                             it gets an SQLException: ResultSet closed (wrapped in a ScriptException of course),
                             although "map.toString()" does work, and this problem happens only in Intellij Idea's
                             debugger, it works as a normal line of code.
                            */
                            String key;
                            String sqlValue;
                            byte sqlValueType;
                            try {
                                key = rs.getString(1);
                                sqlValue = rs.getString(2);
                                sqlValueType = rs.getByte(3);
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }

                            return new Entry<String, ScriptValue<V>>() {

                                @Override
                                public ScriptValue<V> setValue(ScriptValue<V> value) {
                                    throw new UnsupportedOperationException();
                                }

                                @Override
                                public String getKey() {
                                    return key;
                                }

                                @Override
                                public ScriptValue<V> getValue() {
                                    return (ScriptValue<V>) ScriptValueCollection.transformToScriptValue(sqlValue, sqlValueType);
                                }
                            };
                        }              // what a beautiful stair xD
                    };
                }
            };
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @Nullable
    @Override
    public ScriptValue<V> putIfAbsent(String key, ScriptValue<V> value) {
        if (key.length() > 100) throw new IllegalArgumentException("the key in longer than 100 characters");
        ScriptValue<V> previousElement = null;
        if (hashMap != null) previousElement = hashMap.putIfAbsent(key, value);
        else if (! containsKey(key)) {
            previousElement = put(key, value);
        }
        modified();
        return previousElement;
    }

    /**
     * Provides a copy of this ScriptValueMap, but not using SQL and not being the global.
     * The elements themselves are not copied over.
     * @see Object#clone()
     */
    @Override
    public StringScriptValueMap<V> clone() {
        StringScriptValueMap<V> clone = new StringScriptValueMap<>();
        clone.timesModifiedSinceLastSave -= size(); // we don't want it to save anything here
        if (hashMap != null) {
            for (Entry<String, ScriptValue<V>> entry: hashMap.entrySet()) {
                clone.put(entry.getKey(), entry.getValue().clone());
            }
            return clone;
        }
        clone.putAll(this); // element are cloned since the come from a string in SQL
        return clone;
    }

    @Override
    public boolean makeSQL() {
        return makeSQL(false);
    }

    public boolean makeSQL(boolean isTheGlobal) {
        if (hashMap == null) return false;
        if (! Storage.isSQL)
            throw new NotUsingSQLException("the provided Storage class is not using SQL");
        HashMap<String, ScriptValue<V>> copy = hashMap;
        boolean lastIsTheGlobal = this.isTheGlobal;
        StringID StringIDBeforeTry = stringID;

        lastSize = -1;
        this.hashMap = null;
        this.isTheGlobal = isTheGlobal;
        stringID = getNewStringID();
        SQLTable = getFullSQLTableName();
        String query = "CREATE TABLE `" + SQLTable + "` (`name` VARCHAR(100) PRIMARY KEY, `value_object` TEXT, `value_type` TINYINT NOT NULL)";
        try {
            Storage.executeSQLUpdate(query);
            if (isTheGlobal) {
                query = "CREATE UNIQUE INDEX PK_global_vars ON `" + SQLTable + "` (`name`)";
                Storage.executeSQLUpdate(query);
            }
            putAll(copy);
            return true;
        } catch (SQLException e) {
            hashMap = copy;
            this.isTheGlobal = lastIsTheGlobal;
            stringID = StringIDBeforeTry;
            SQLTable = null;
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    private void modified() {
        timesModifiedSinceLastSave++;
        if (timesModifiedSinceLastSave >= Storage.getJsonSaveInterval()) {
            Storage.jsonSave();
            timesModifiedSinceLastSave = 0;
        }
        lastSize = -1;
    }

    /**
     * Contrary of {@link ScriptValueMap#toNormalClasses(boolean)}
     * @param keysAreJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
     *
     */
    public static <V> StringScriptValueMap<? extends V> toScriptValues(Map<String, V> map, boolean keysAreJson) {
        StringScriptValueMap<V> finalMap = new StringScriptValueMap<>();
        finalMap.timesModifiedSinceLastSave -= map.size();
        for (Entry<String, V> entry: map.entrySet()) {
            finalMap.put(entry.getKey(), (ScriptValue<V>) ScriptValue.toScriptValue(entry.getValue(), keysAreJson));
        }
        return finalMap;
    }

    /**
     * Contrary of {@link ScriptValueMap#toScriptValues(Map, boolean)}
     */
    @Override
    public HashMap<String, Object> toNormalClasses(boolean forJson) {
        HashMap<String, Object> finalMap = new HashMap<>(size());
        for (Entry<String, ScriptValue<V>> entry : entrySet()) {
            finalMap.put(entry.getKey(), entry.getValue().toNormalClass(forJson));
        }
        return finalMap;
    }

    @Override
    public @Nullable String getSQLTable() {
        return SQLTable;
    }

    @Override
    public boolean canUseSQL() {
        return useSQLIfPossible && Storage.isSQL;
    }

    @Override
    public int howManyTimesModifiedSinceLastSave() {
        return timesModifiedSinceLastSave;
    }

    @Override
    public @NotNull StringID getStringID() {
        return stringID;
    }

    /**
     * used just for preventing code duplication in the constructors, after you can just use {@link #getSQLTable()}
     */
    private String getFullSQLTableName() {
        return getFullSQLTableName(stringID, isTheGlobal);
    }

    /**
     * used just for preventing code duplication in the constructors, after you can just use {@link #getSQLTable()}
     * @return {@code table_prefix + "d" + stringID}
     */
    private static String getFullSQLTableName(StringID stringID, boolean isTheGlobal) {
        if (isTheGlobal) return ((HashMap<String, Object>) Storage.getHashMap().get("SQLite")).get("table-prefix") + "global_vars";
        if (! Storage.isSQL) throw new NotUsingSQLException("Storage class is doesn't use SQL");
        return (String) ((HashMap<String, Object>) Storage.getHashMap().get("SQLite")).get("table-prefix") + stringID;
    }

    @Override
    public char getTypeChar() {
        return typeChar;
    }
}
