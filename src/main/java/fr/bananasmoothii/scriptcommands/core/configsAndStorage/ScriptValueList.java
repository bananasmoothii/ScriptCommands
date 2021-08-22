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

import fr.bananasmoothii.scriptcommands.core.contextReplacement.AbstractScriptValueList;
import fr.bananasmoothii.scriptcommands.core.contextReplacement.UseContext;
import fr.bananasmoothii.scriptcommands.core.execution.Context;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptException;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import fr.bananasmoothii.scriptcommands.core.execution.Types;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


@SuppressWarnings({"unchecked"})
public class ScriptValueList<E> extends AbstractScriptValueList<E> {

    /** with prefix, null if isSQL is false. */
    private @Nullable String SQLTable;

    /** @see ScriptValueCollection#getSQLTable() */
    public static final char typeChar = 'l';

    private int timesModifiedSinceLastSave = 1;
    /**
     * Not null only with json, use {@code arrayList == null} to know if this list (class) is using SQL.
     */
    private @Nullable List<ScriptValue<E>> internalList;

    private final Object modificationLock = new Object();

    private @NotNull StringID stringID;
    
    private boolean useSQLIfPossible;

    private static StringID lastStringID;

    public ScriptValueList() {
        this(false);
    }

    /**
     * @param useSQLIfPossible if null, everything will be stored in a normal {@link ArrayList}
     */
    public ScriptValueList(boolean useSQLIfPossible) {
        super();
        this.useSQLIfPossible = useSQLIfPossible;
        // from here to the creation of the SQL table, "this" will throw an error in debuggers because the table doesn't exist, so size() or toString() won't work
        stringID = getNewStringID();
        if (canUseSQL()) {
            SQLTable = getFullSQLTableName();
            String query = "CREATE TABLE " + SQLTable + " (`index` INT PRIMARY KEY, `object` TEXT, `type` TINYINT NOT NULL)";
            try {
                Storage.executeSQLUpdate(query);
            } catch (SQLException e) {
                throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
            }
        } else {
            internalList = new ArrayList<>();
        }
        modified();
    }

    public ScriptValueList(Collection<? extends ScriptValue<E>> c) {
        this();
        addAll(c);
    }

    public ScriptValueList(@NotNull String SQLTable) {
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
    public ScriptValueList(@NotNull StringID stringID) {
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
    public int size(@Nullable Context context) {
        if (internalList != null) return internalList.size();
        if (lastSize != -1 && timesModifiedSinceLastSave == 0) return lastSize;
        String query = "SELECT COUNT(*) FROM `" + SQLTable + '`';
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            rs.next();
            lastSize = rs.getInt(1);
            return lastSize;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    @Override
    @Contract(value = "_, _->true", mutates = "this") // as in List#add(E)
    public boolean add(ScriptValue<E> element, @Nullable Context context) {
        if (internalList != null) {
            synchronized (modificationLock) {
                internalList.add(element);
            }
            modified();
            return true;
        }
        String query = "INSERT INTO `" + SQLTable + "` VALUES(?, ?, ?)";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setInt(1, size());
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, element, 2, 3);
            query += " => " + ps;
            ps.executeUpdate();
            modified();
            return true;
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    @Override
    public void add(int index, ScriptValue<E> element, @Nullable Context context) {
        rangeCheckForAdd(index, context);
        if (internalList != null) {
            synchronized (modificationLock) {
                internalList.add(index, element);
            }
            modified();
            return;
        }
        String query = "UPDATE `" + SQLTable + "` SET `index` = `index` + 1 WHERE `index` >= " + index;
        try {
            Storage.executeSQLUpdate(query);
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
        query = "INSERT INTO `" + SQLTable + "` VALUES(?, ?, ?)";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setInt(1, index);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, element, 2, 3);
            query += " => " + ps;
            ps.executeUpdate();
            modified();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    @Override
    public ScriptValue<E> set(int index, ScriptValue<E> element, @Nullable Context context) {
        rangeCheck(index, context);
        if (internalList != null) {
            ScriptValue<E> ret;
            synchronized (modificationLock) {
                ret = internalList.set(index, element);
            }
            modified();
            return ret;
        }
        ScriptValue<E> previousElement = get(index, context);
        String query = "UPDATE `" + SQLTable + "` SET `object` = ?, `type` = ? WHERE `index` = ?";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setInt(3, index);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, element, 1, 2);
            ps.executeUpdate();
            query += " => " + ps;
            modified();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
        return previousElement;
    }

    /**
     * If this returns {@code null}, it means there was an error, because if this works, it should return
     * a <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code>
     */
    @Override
    public ScriptValue<E> get(int index, @Nullable Context context) {
        rangeCheck(index, context);
        if (internalList != null) {
            return internalList.get(index);
        }
        String query = "SELECT `object`, `type` FROM `" + SQLTable + "` WHERE `index` = " + index;
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            rs.next();
            return (ScriptValue<E>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2));
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list. <br>
     * If this returns {@code null}, it means there was an error, because if this works, it should return
     * a <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code>
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @see ScriptValueList#get(int, Context)
     */
    public ScriptValue<E> remove(int index, @Nullable Context context) {
        rangeCheck(index, context);
        if (internalList != null) {
            ScriptValue<E> ret;
            synchronized (modificationLock) {
                ret = internalList.remove(index);
            }
            modified();
            return ret;
        }
        ScriptValue<E> previous = get(index, context);
        String query = "DELETE FROM `" + SQLTable + "` WHERE `index` = " + index;
        try {
            Storage.executeSQLUpdate(query);
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query);
        }
        query = "UPDATE `" + SQLTable + "` SET `index` = `index` - 1 WHERE `index` > " + index;
        try {
            Storage.executeSQLUpdate(query);
            modified();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
        return previous;
    }

    @Override
    public boolean remove(Object o, @Nullable Context context) {
        if (! (o instanceof ScriptValue)) return false;
        if (internalList != null) {
            boolean ret;
            synchronized (modificationLock) {
                ret = internalList.remove(o);
            }
            modified();
            return ret;
        }
        int index = indexOf(o, context);
        if (index == -1) return false;
        remove(index, context);
        return true;
    }

    @Override
    public int indexOf(Object o, @Nullable Context context) {
        if (! (o instanceof ScriptValue)) return -1;
        if (internalList != null) {
            return internalList.indexOf(o);
        }
        return indexOf(o, "ASC", context);
    }

    @Override
    public int lastIndexOf(Object o, @Nullable Context context) {
        if (! (o instanceof ScriptValue)) return -1;
        if (internalList != null) {
            return internalList.lastIndexOf(o);
        }
        return indexOf(o, "DESC", context);
    }

    private int indexOf(Object o, String order, @Nullable Context context) {
        ScriptValue<?> element = (ScriptValue<?>) o;
        String query = "SELECT `index` FROM `" + SQLTable + "` WHERE `object` = ? AND `type` = ? ORDER BY `index` " + order + " LIMIT 1";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, element, 1, 2);
            query += " => " + ps;
            ResultSet resultSet = ps.executeQuery();
            if (! resultSet.next()) return -1;
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    @Override
    public void clear(@Nullable Context context) {
        if (!isEmpty(context)) return;

        if (internalList != null) {
            synchronized (modificationLock) {
                internalList.clear();
            }
            modified();
            return;
        }
        String query = "DELETE FROM `" + SQLTable + '`';
        try {
            Storage.executeSQLUpdate(query);
            modified();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    @Override
    public @NotNull ScriptValueList<E> subList(int fromIndex, int toIndex, @Nullable Context context) {
        rangeCheck(fromIndex, context);
        rangeCheck(toIndex - 1, context);
        ScriptValueList<E> newList = new ScriptValueList<>();
        if (internalList != null) {
            for (; fromIndex < toIndex; fromIndex++) {
                newList.add(get(fromIndex, context).clone());
            }
            return newList;
        }
        String query = "SELECT `object`, `type` FROM `" + SQLTable + "` WHERE `index` BETWEEN " + fromIndex + " AND " + (toIndex - 1) + " ORDER BY `index`";
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            while (rs.next()) {
                newList.add((ScriptValue<E>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2)), context);
            }
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
        return newList;
    }

    @Override @UseContext
    public ScriptValueList<E> subList(int fromIndex, int toIndex) {
        return (ScriptValueList<E>) super.subList(fromIndex, toIndex);
    }

    @Override
    public boolean contains(Object o, @Nullable Context context) {
        if (! (o instanceof ScriptValue)) return false;
        if (internalList != null) return internalList.contains(o);
        String query = "SELECT * FROM `" + SQLTable + "` WHERE `object` = ? AND `type` = ? LIMIT 1";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, (ScriptValue<?>) o, 1, 2);
            query += " => " + ps;
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }


    /**
     * Provides a copy of this ScriptValueList, but not using SQL and with no {@link Storage}.
     * The elements themselves are not copied over.
     * @see Object#clone()
     */
    @Override
    public ScriptValueList<E> clone() {
        ScriptValueList<E> clone;
        synchronized (modificationLock) {
            clone = new ScriptValueList<>();
            clone.timesModifiedSinceLastSave -= size(); // we don't want it to save anything here
            if (internalList != null) {
                for (ScriptValue<E> scriptValue : this) {
                    clone.add(scriptValue.clone(), context);
                }
                return clone;
            }
            clone.addAll(this, context); // element are cloned since the come from a string in SQL
        }
        return clone;
    }

    @Override
    public synchronized boolean makeSQL(@Nullable Context context) {
        if (internalList == null) return false;
        if (! Storage.isSQL)
            throw new NotUsingSQLException("the provided Storage class is not using SQL");
        List<ScriptValue<E>> copy = internalList;
        StringID StringIDBeforeTry = stringID;

        lastSize = -1;
        internalList = null;
        stringID = getNewStringID();
        SQLTable = getFullSQLTableName();
        String query = "CREATE TABLE " + SQLTable + " (`index` INT PRIMARY KEY, `object` TEXT, `type` TINYINT NOT NULL)";
        try {
            Storage.executeSQLUpdate(query);
            addAll(copy, context);
            return true;
        } catch (SQLException e) {
            internalList = copy;
            stringID = StringIDBeforeTry;
            SQLTable = null;
            throw ScriptException.Incomplete.wrapInShouldNotHappen(e, query).completeIfPossible(context);
        }
    }

    /**
     * Do not synchronise that method
     */
    private void modified() {
        synchronized (modificationLock) {
            timesModifiedSinceLastSave++;
            if (timesModifiedSinceLastSave >= Storage.getJsonSaveInterval()) {
                Storage.jsonSave();
                timesModifiedSinceLastSave = 0;
            }
            lastSize = -1;
        }
    }

    /**
     * Contrary of {@link #toScriptValues(Object[], boolean)}
     */
    public List<ScriptValue<E>> toNormalList() {
        if (internalList != null) return internalList; // could be if !isSQL() too, theoretically
        ArrayList<ScriptValue<E>> a = new ArrayList<>(size());
        a.addAll(this);
        return a;
    }

    /**
     * Contrary of {@link #toNormalList()}
     * @see #toScriptValues(Object[], boolean)
     */
    public static <T> ScriptValueList<T> toScriptValues(T[] elements) {
        return toScriptValues(elements, false);
    }

    /**
     * Contrary of {@link #toNormalList()}
     * @param keysAreJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
     */
    public static <T> ScriptValueList<T> toScriptValues(T[] elements, boolean keysAreJson) {
        ScriptValueList<T> list = new ScriptValueList<>();
        list.timesModifiedSinceLastSave -= elements.length;
        for (T element: elements) {
            list.add((ScriptValue<T>) ScriptValue.toScriptValue(element, keysAreJson));
        }
        return list;
    }

    /**
     * Contrary of {@link ScriptValueList#toNormalClasses(boolean)}
     */
    public static <T> ScriptValueList<? extends T> toScriptValues(List<T> list) {
        return toScriptValues(list, false);
    }

    /**
     * Contrary of {@link ScriptValueList#toNormalClasses(boolean)}
     * @param keysAreJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
     *
     */
    public static <T> ScriptValueList<? extends T> toScriptValues(Collection<T> collection, boolean keysAreJson) {
        return (ScriptValueList<? extends T>) toScriptValues(collection.toArray(), keysAreJson);
    }

    /**
     * contrary of {@link ScriptValueList#toScriptValues(Collection, boolean)}
     */
    @Override
    @Contract(pure = true)
    public ArrayList<E> toNormalClasses(boolean forJson) {
        ArrayList<E> finalList = new ArrayList<>(size());
        for (ScriptValue<E> element: this) {
            finalList.add((E) element.toNormalClass(forJson));
        }
        return finalList;
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
     * @return {@code table_prefix + "l" + stringID}
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
}
