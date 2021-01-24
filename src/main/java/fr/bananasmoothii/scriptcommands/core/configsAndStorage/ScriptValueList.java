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
 * @param <E> the type of each {@link ScriptValue}
 */
@SuppressWarnings({"unchecked"})
public class ScriptValueList<E> extends AbstractList<ScriptValue<E>> implements ScriptValueCollection, Iterable<ScriptValue<E>> {

    /** with prefix, null if isSQL is false. */
    private @Nullable String SQLTable;

    /** @see ScriptValueCollection#getSQLTable() */
    public static final char typeChar = 'l';

    private int timesModifiedSinceLastSave = 1;
    /**
     * Not null only with json, use {@code arrayList == null} to know if this list (class) is using SQL.
     */

    private @Nullable ArrayList<ScriptValue<E>> arrayList;

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
                throw ScriptException.toScriptException(e, 1, query);
            }
        } else {
            arrayList = new ArrayList<>();
        }
        modified();
    }

    public ScriptValueList(Collection<ScriptValue<E>> c) {
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
    public int size() {
        if (arrayList != null) return arrayList.size();
        if (lastSize != -1 && timesModifiedSinceLastSave == 0) return lastSize;
        String query = "SELECT COUNT(*) FROM `" + SQLTable + '`';
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            rs.next();
            lastSize = rs.getInt(1);
            return lastSize;
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, 1, query);
        }
    }

    @Override
    public boolean add(ScriptValue<E> element) {
        if (element.v == this) throw new ElementIsCollectionException();
        if (arrayList != null) {
            boolean returned = arrayList.add(element);
            if (returned) modified();
            return returned;
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
            throw ScriptException.toScriptException(e, query);
        }
    }

    @Override
    public void add(int index, ScriptValue<E> element) {
        if (element.v == this) throw new ElementIsCollectionException();

        if (arrayList != null) {
            arrayList.add(index, element);
            modified();
            return;
        }
        rangeCheck(index);
        String query = "UPDATE `" + SQLTable + "` SET `index` = `index` + 1 WHERE `index` >= " + index;
        try {
            Storage.executeSQLUpdate(query);
        } catch (SQLException e) {
            ScriptException.appendToSQLExceptionQuery(e, query);
            e.printStackTrace();
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
            throw ScriptException.toScriptException(e, query);
        }
    }

    @Override
    public ScriptValue<E> set(int index, ScriptValue<E> element) {
        if (arrayList != null) {
            ScriptValue<E> ret = arrayList.set(index, element);
            modified();
            return ret;
        }
        ScriptValue<E> previousElement = get(index);
        rangeCheck(index);
        String query = "UPDATE `" + SQLTable + "` SET `object` = ?, `type` = ? WHERE `index` = ?";
        try {
            PreparedStatement ps = Storage.prepareSQLStatement(query);
            ps.setInt(3, index);
            ScriptValueCollection.setScriptValueInPreparedStatement(ps, element, 1, 2);
            ps.executeUpdate();
            query += " => " + ps;
            modified();
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
        return previousElement;
    }

    /**
     * Checks if the given index is in range.  If not, throws an appropriate
     * runtime exception.  This method does *not* check if the index is
     * negative: It is always used immediately prior to an array access,
     * which throws an ArrayIndexOutOfBoundsException if index is negative.
     * @see ArrayList#remove(int)
     */
    private void rangeCheck(int index) {
        if (index >= size())
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }

    /*public boolean addAll(ScriptValueList<E> c) {
        return super.addAll(c);
    }*/

    /**
     * If this returns {@code null}, it means there was an error, because if this works, it should return
     * a <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code>
     */
    @Override
    public ScriptValue<E> get(int index) {
        if (arrayList != null) {
            return arrayList.get(index);
        }
        rangeCheck(index);
        String query = "SELECT `object`, `type` FROM `" + SQLTable + "` WHERE `index` = " + index;
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            rs.next();
            return (ScriptValue<E>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2));
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.<br/>
     * If this returns {@code null}, it means there was an error, because if this works, it should return
     * a <code>{@link ScriptValue}<{@link fr.bananasmoothii.scriptcommands.core.execution.NoneType NoneType}></code>
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @see ScriptValueList#get(int)
     */
    public ScriptValue<E> remove(int index) {
        if (arrayList != null) {
            ScriptValue<E> ret = arrayList.remove(index);
            modified();
            return ret;
        }
        rangeCheck(index);
        ScriptValue<E> previous = get(index);
        String query = "DELETE FROM `" + SQLTable + "` WHERE `index` = " + index;
        try {
            Storage.executeSQLUpdate(query);
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
        query = "UPDATE `" + SQLTable + "` SET `index` = `index` - 1 WHERE `index` > " + index;
        try {
            Storage.executeSQLUpdate(query);
            modified();
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
        return previous;
    }

    @Override
    public void clear() {
        for (int i = size() - 1; i >= 0; i--) {
            remove(i);
        }
        modified();
    }

    /**
     * This is a bit different than {@link AbstractList#subList(int, int) the default subList} because here,
     * it is already a {@link ScriptValueList}, and a {@link #clone()} equivalent was applied.
     * @param fromIndex inclusive
     * @param toIndex exclusive
     */
    @Override
    public @NotNull ScriptValueList<E> subList(int fromIndex, int toIndex) {
        rangeCheck(fromIndex);
        rangeCheck(toIndex - 1);
        ScriptValueList<E> newList = new ScriptValueList<>();
        if (arrayList != null) {
            for (; fromIndex < toIndex; fromIndex++) {
                newList.add(get(fromIndex).clone());
            }
            return newList;
        }
        String query = "SELECT `object`, `type` FROM `" + SQLTable + "` WHERE `index` BETWEEN " + fromIndex + " AND " + (toIndex - 1) + " ORDER BY `index`";
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            while (rs.next()) {
                newList.add((ScriptValue<E>) ScriptValueCollection.transformToScriptValue(rs.getString(1), rs.getByte(2)));
            }
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
        return newList;
    }

    @Override
    public boolean contains(Object o) {
        if (! (o instanceof ScriptValue)) return false;
        if (arrayList != null) return arrayList.contains(o);
        String query = "SELECT `object`, `type` FROM `" + SQLTable + '`';
        ScriptValue<?> o1 = (ScriptValue<?>) o;
        try {
            ResultSet rs = Storage.executeSQLQuery(query);
            while (rs.next()) {
                byte type = rs.getByte(2);
                if (type != o1.typeByte) continue;
                ScriptValue<?> objectTested = ScriptValueCollection.transformToScriptValue(rs.getString(1), type);
                if (objectTested.equals(o1)) return true;
            }
            return false;
        } catch (SQLException e) {
            throw ScriptException.toScriptException(e, query);
        }
    }

    @Override
    public @NotNull Iterator<ScriptValue<E>> iterator() {
        return new Iterator<ScriptValue<E>>() {
            private int currentIndex = 0;
            private final int size = size();

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public ScriptValue<E> next() {
                return get(currentIndex++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("This iterator is read only");
            }
        };
    }

    /**
     * Provides a copy of this ScriptValueList, but not using SQL and with no {@link Storage}.
     * The elements themselves are not copied over.
     * @see Object#clone()
     */
    @Override
    public ScriptValueList<E> clone() {
        ScriptValueList<E> clone = new ScriptValueList<>();
        clone.timesModifiedSinceLastSave -= size(); // we don't want it to save anything here
        if (arrayList != null) {
            for (ScriptValue<E> scriptValue: this) {
                clone.add(scriptValue.clone());
            }
            return clone;
        }
        clone.addAll(this); // element are cloned since the come from a string in SQL
        return clone;

        /*
        try {
            ScriptValueList<E> clone = (ScriptValueList<E>) super.clone();
            clone.useSQLIfPossible = false;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw ScriptException.toScriptException(e);
        }

         */
    }

    @Override
    public boolean makeSQL() {
        if (arrayList == null) return false;
        if (! Storage.isSQL)
            throw new NotUsingSQLException("the provided Storage class is not using SQL");
        ArrayList<ScriptValue<E>> copy = arrayList;
        StringID StringIDBeforeTry = stringID;

        lastSize = -1;
        arrayList = null;
        stringID = getNewStringID();
        SQLTable = getFullSQLTableName();
        String query = "CREATE TABLE " + SQLTable + " (`index` INT PRIMARY KEY, `object` TEXT, `type` TINYINT NOT NULL)";
        try {
            Storage.executeSQLUpdate(query);
            addAll(copy);
            return true;
        } catch (SQLException e) {
            arrayList = copy;
            stringID = StringIDBeforeTry;
            SQLTable = null;
            throw ScriptException.toScriptException(e, query);
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
     * Contrary of {@link #toScriptValues(Object[], boolean)}
     */
    public ArrayList<ScriptValue<E>> toArrayList() {
        if (arrayList != null) return arrayList; // could be if !isSQL() too, theoretically
        ArrayList<ScriptValue<E>> a = new ArrayList<>(size());
        a.addAll(this);
        return a;
    }

    /**
     * Contrary of {@link #toArrayList()}
     * @see #toScriptValues(Object[], boolean)
     */
    public static <T> ScriptValueList<T> toScriptValues(T[] elements) {
        return toScriptValues(elements, false);
    }

    /**
     * Contrary of {@link #toArrayList()}
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
     */
    public static <T> ScriptValueList<? extends T> toScriptValues(List<T> list, boolean keysAreJson) {
        return (ScriptValueList<? extends T>) toScriptValues(list.toArray(), keysAreJson);
    }

    /**
     * contrary of {@link ScriptValueList#toScriptValues(List, boolean)}
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
