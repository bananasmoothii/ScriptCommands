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
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import fr.bananasmoothii.scriptcommands.core.execution.Types;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @param <K> the key type of each {@link ScriptValue}
 * @param <V> the value type of each {@link ScriptValue}
 */
@SuppressWarnings({"unchecked"})
public class ScriptValueMap<K, V> extends AbstractMap<ScriptValue<K>, ScriptValue<V>> implements ScriptValueCollection {

    /** with prefix, null if isSQL is false. */
    private @Nullable String SQLTable;

    /** @see ScriptValueCollection#getSQLTable() */
    public static final char typeChar = 'd';

    private int timesModifiedSinceLastSave = 1;

    /**
     * Not null only with json, use {@code hashMap == null} to know if this map (class) is using SQL.
     */
    private @Nullable HashMap<ScriptValue<K>, ScriptValue<V>> hashMap;

    private @NotNull StringID stringID;

    private boolean useSQLIfPossible;

    private static StringID lastStringID;


    public ScriptValueMap() {
        this(false);
    }

    /**
     * @param useSQLIfPossible if false, everything will be stored in a normal {@link HashMap}
     */
    public ScriptValueMap(boolean useSQLIfPossible) {
        super();
        this.useSQLIfPossible = useSQLIfPossible;
        // from here to the creation of the SQL table, "this" will throw an error in debuggers because the table doesn't exist, so size() or toString() won't work
        stringID = getNewStringID();
        if (canUseSQL()) {
            SQLTable = getFullSQLTableName();
            String query = "CREATE TABLE `" + SQLTable + "` (`key_object` TEXT, `key_type` TINYINT NOT NULL, `value_object` TEXT, `value_type` TINYINT NOT NULL)";
            try {
                Storage.executeSQLUpdate(query);
            } catch (SQLException e) {
                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
            }
        } else {
            hashMap = new HashMap<>();
        }
        modified();
    }

    public ScriptValueMap(Map<ScriptValue<K>, ScriptValue<V>> map) {
        this();
        putAll(map);
    }

    public ScriptValueMap(@NotNull String SQLTable) {
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
    public ScriptValueMap(@NotNull StringID stringID) {
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
            while (Storage.sqlTableExists(getFullSQLTableName(currentID))) {
                currentID = currentID.nextID();
            }
        }
        lastStringID = currentID;
        return currentID;
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
        if (! (key instanceof ScriptValue)) return false;
        if (hashMap != null) return hashMap.containsKey(key);
        String query = "SELECT `key_object`, `key_type` FROM `" + SQLTable + '`';
        ScriptValue<?> key1 = (ScriptValue<?>) key;
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            while (rs.next()) {
                byte type = rs.getByte(2);
                if (type != key1.type.asByte) continue;
                ScriptValue<?> objectTested = ScriptValueCollection.transformToScriptValue(rs.getString(1), type);
                if (objectTested.equals(key1)) return true;
            }
            return false;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    /**
     * If this returns {@code null}, it means there was an error or your key wasn't a {@link ScriptValue},
     * because if this works, it should return a
     * <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code> <br>
     * @return the object if found, a {@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType}>
     *     otherwise
     */
    @Override
    public ScriptValue<V> get(Object key) {
        if (! (key instanceof ScriptValue)) return null;
        if (hashMap != null) return hashMap.get(key);
        String query = "SELECT `value_object`, `value_type` FROM `" + SQLTable + "` WHERE `key_object` " + getSQLEqualsSign((ScriptValue<?>) key) + " ? AND `key_type` = ?";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, (ScriptValue<V>) key, 1, 2);
            query += " => " + ps;
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return new ScriptValue<>(null);
            return (ScriptValue<V>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2));
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @Override
    public ScriptValue<V> put(ScriptValue<K> key, ScriptValue<V> value) {
        ScriptValue<V> previousElement;
        if (hashMap != null) previousElement = hashMap.put(key, value);
        else {
            if (containsKey(key)) {
                previousElement = get(key);
            } else {
                previousElement = new ScriptValue<>(null);
            }
            if (containsKey(key)) {
                String query = "UPDATE `" + SQLTable + "` SET `value_object` = ?, `value_type` = ? WHERE `key_object` " + getSQLEqualsSign(key) + " ? AND `key_type` = ?";
                try {
                    PreparedStatement ps = Storage.prepareSQLStatement(query);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, value, 1, 2);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, key, 3, 4);
                    query += " => " + ps;
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                }
            } else {
                String query = "INSERT INTO `" + SQLTable + "` VALUES(?, ?, ?, ?)";
                try {
                    PreparedStatement ps = Storage.prepareSQLStatement(query);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, key, 1, 2);
                    ScriptValueCollection.setScriptValueInPreparedStatement(ps, value, 3, 4);
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
        ScriptValue<V> previousElement = get(key);
        if (hashMap != null) previousElement = hashMap.remove(key);
        else {
            String query = "DELETE FROM `" + SQLTable + "` WHERE `key_object` " + getSQLEqualsSign((ScriptValue<?>) key) + " ? AND `key_type` = ?";
            try {
                PreparedStatement ps = Storage.prepareSQLStatement(query);
                ScriptValueCollection.setScriptValueInPreparedStatement(ps, (ScriptValue<K>) key, 1, 2);
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
        for (ScriptValue<K> s: keySet()) {
            remove(s);
        }
        modified();
    }

    @NotNull
    @Override
    public Set<ScriptValue<K>> keySet() {
        if (hashMap != null) return hashMap.keySet();
        String query = "SELECT `key_object`, `key_type` FROM `" + SQLTable + '`';
        try {
            ResultSet rs1 = Storage.executeSQLQuery(query);
            int size1 = size();
            return new AbstractSet<ScriptValue<K>>() {
                final int size = size1;
                final ResultSet rs = rs1;

                @Override
                public @NotNull Iterator<ScriptValue<K>> iterator() {
                    return new Iterator<ScriptValue<K>>() {
                        @Override
                        public boolean hasNext() {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }
                        }

                        @Override
                        public ScriptValue<K> next() {
                            try {
                                return (ScriptValue<K>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2));
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
                list.add((ScriptValue<V>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2)));//, context);
            }
            return list;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
    }

    @NotNull
    @Override
    public Set<Entry<ScriptValue<K>, ScriptValue<V>>> entrySet() {
        if (hashMap != null) return hashMap.entrySet();
        String query = "SELECT * FROM `" + SQLTable + '`';
        try {
            ResultSet rs1 = Storage.executeSQLQuery(query);
            int size1 = size();
            // sry for debugging, with all these nested lambdas xD
            return new AbstractSet<Entry<ScriptValue<K>, ScriptValue<V>>>() {
                final int size = size1;
                final ResultSet rs = rs1;

                @Override
                public int size() {
                    return size;
                }

                @Override
                public @NotNull Iterator<Entry<ScriptValue<K>, ScriptValue<V>>> iterator() {
                    return new Iterator<Entry<ScriptValue<K>, ScriptValue<V>>>() {
                        @Override
                        public boolean hasNext() {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }
                        }

                        @Override
                        public Entry<ScriptValue<K>, ScriptValue<V>> next() {
                            /*
                             For some cloudy reason, when you do the "try return ResultSet.get... catch" block inside
                             the getKey and getValue methods, the debugger can't look at what's inside of the map,
                             it gets an SQLException: ResultSet closed (wrapped in a ScriptException of course),
                             although "map.toString()" does work, and this problem happens only in Intellij Idea's
                             debugger, it works as a normal line of code.
                            */
                            String sqlKey;
                            byte sqlKeyType;
                            String sqlValue;
                            byte sqlValueType;
                            try {
                                sqlKey = rs.getString(1);
                                sqlKeyType = rs.getByte(2);
                                sqlValue = rs.getString(3);
                                sqlValueType = rs.getByte(4);
                            } catch (SQLException e) {
                                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
                            }

                            return new Entry<ScriptValue<K>, ScriptValue<V>>() {

                                @Override
                                public ScriptValue<V> setValue(ScriptValue<V> value) {
                                    throw new UnsupportedOperationException();
                                }

                                @Override
                                public ScriptValue<K> getKey() {
                                    return (ScriptValue<K>) ScriptValueCollection.transformToScriptValue(sqlKey, sqlKeyType);
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
    public ScriptValue<V> putIfAbsent(ScriptValue<K> key, ScriptValue<V> value) {
        ScriptValue<V> ret = null;
        if (hashMap != null) ret = hashMap.putIfAbsent(key, value);
        else if (! containsKey(key)) {
            ret = put(key, value);
        }
        modified();
        return ret;
    }

    /**
     * Provides a copy of this ScriptValueMap, but not using SQL
     * The elements themselves are not copied over.
     * @see Object#clone()
     */
    @Override
    public ScriptValueMap<K, V> clone() {
        ScriptValueMap<K, V> clone = new ScriptValueMap<>();
        clone.timesModifiedSinceLastSave -= size(); // we don't want it to save anything here
        if (hashMap != null) {
            for (Entry<ScriptValue<K>, ScriptValue<V>> entry: hashMap.entrySet()) {
                clone.put(entry.getKey().clone(), entry.getValue().clone());
            }
            return clone;
        }
        clone.putAll(this); // element are cloned since the come from a string in SQL
        return clone;
    }

    @Override
    public boolean makeSQL() {
        if (hashMap == null) return false;
        if (! Storage.isSQL)
            throw new NotUsingSQLException("the provided Storage class is not using SQL");
        HashMap<ScriptValue<K>, ScriptValue<V>> copy = hashMap;
        StringID StringIDBeforeTry = stringID;

        lastSize = -1;
        hashMap = null;
        stringID = getNewStringID();
        SQLTable = getFullSQLTableName();
        String query = "CREATE TABLE `" + SQLTable + "` (`key_object` TEXT, `key_type` TINYINT NOT NULL, `value_object` TEXT, `value_type` TINYINT NOT NULL)";
        try {
            Storage.executeSQLUpdate(query);
            putAll(copy);
            return true;
        } catch (SQLException e) {
            hashMap = copy;
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
     * Contrary of {@link ScriptValueMap#toNormalClasses()}
     * This uses {@code <K, V>} because a {@link ScriptValueMap}{@code <K, V>} extends {@link Map}{@code <K, V>}.
     * @param keysAreJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
     *
     */
    public static <K, V> ScriptValueMap<? extends K, ? extends V> toScriptValues(Map<K, V> map, boolean keysAreJson) {
        ScriptValueMap<K, V> finalMap = new ScriptValueMap<>();
        finalMap.timesModifiedSinceLastSave -= map.size(); // we don't want it to save anything here
        for (Entry<K, V> entry: map.entrySet()) {
            K normalKey = entry.getKey();
            ScriptValue<K> key;
            if (keysAreJson) {
                Gson gson = Storage.gson;
                String normalKeyString = normalKey.toString();
                switch (normalKeyString.charAt(0)) {
                    case '-':
                        key = (ScriptValue<K>) new ScriptValue<>(normalKeyString.substring(1));
                        break;
                    case '/':
                        key = (ScriptValue<K>) ScriptValue.notCollectionToScriptValue(normalKeyString.substring(1));
                        break;
                    case '_':
                        key = (ScriptValue<K>) ScriptValue.toScriptValue(
                                gson.fromJson(normalKeyString.substring(1), Object.class), // class of T should be K.class, but that doesn't work...
                                true);
                        break;
                    case '!':
                        key = (ScriptValue<K>) ScriptValue.NONE;
                        break;
                    default:
                        throw new IllegalStateException("Invalid json type identifier, it should be '-', '_', '/' or '!' but it is "
                                + normalKeyString.charAt(0));
                }
            } else {
                key = (ScriptValue<K>) ScriptValue.toScriptValue(normalKey, false);
            }
            finalMap.put(key, (ScriptValue<V>) ScriptValue.toScriptValue(entry.getValue(), keysAreJson));
        }
        return finalMap;
    }

    /**
     * Contrary of {@link ScriptValueMap#toScriptValues(Map, boolean)}. <br>
     * Class type are {@code <Object, Object>} because for example, the normal class of
     * {@link ScriptValueMap} is {@link HashMap}
     * @param forJson if true, it will return {@code '/' + }{@link Gson#toJson(Object)} for keys that are not collections
     *                ({@link ScriptValueCollection}), {@code '_' + }{@link Object#toString()} for keys that are
     *                collections, {@code '-' + }{@link Object#toString()} for keys that are {@link String}
     *                and {@code "!"} for {@link fr.bananasmoothii.scriptcommands.core.execution.NoneType None}
     *                / {@code null} keys. If false, keys will just have {@link ScriptValue#toNormalClass(boolean)}.
     */
    @Override
    public HashMap<Object, Object> toNormalClasses(boolean forJson) {
        HashMap<Object, Object> finalMap = new HashMap<>(size());
        for (Entry<ScriptValue<K>, ScriptValue<V>> entry : entrySet()) {
            Object normalKey = entry.getKey().toNormalClass(forJson);
            if (forJson) {
                if (normalKey instanceof List || normalKey instanceof Map) {
                    Gson gson = Storage.gson;
                    normalKey = '_' + gson.toJson(normalKey);
                } else if (normalKey instanceof String) {
                    normalKey = '-' + normalKey.toString();
                } else if (normalKey != null) {
                     normalKey = '/' + normalKey.toString();
                } else {
                    normalKey = "!";
                }
            }
            finalMap.put(normalKey, entry.getValue().toNormalClass(forJson));
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
        return getFullSQLTableName(stringID);
    }

    /**
     * used just for preventing code duplication in the constructors, after you can just use {@link #getSQLTable()}
     * @return {@code table_prefix + "d" + stringID}
     */
    private static String getFullSQLTableName(StringID stringID) {
        if (! Storage.isSQL) throw new NotUsingSQLException("Storage class is doesn't use SQL");
        return (String) ((HashMap<String, Object>) Storage.getHashMap().get("SQLite")).get("table-prefix") + stringID;
    }

    @Override
    public char getTypeChar() {
        return typeChar;
    }

    @Override
    public String toString() {
        return Types.getPrettyArg(new ScriptValue<>(this));
    }

    /**
     * @return {@code object.type.equals("None") ? "IS" : "="}
     */
    public static String getSQLEqualsSign(ScriptValue<?> object) {
        return object.is(ScriptValue.ScriptValueType.NONE) || (object.is(ScriptValue.ScriptValueType.BOOLEAN) && ! object.asBoolean()) ? "IS" : "=";
    }
}
