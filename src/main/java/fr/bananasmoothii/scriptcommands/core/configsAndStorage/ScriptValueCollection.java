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

import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ScriptValueCollection extends Cloneable {

    /*
    /**
     * This will clone the collection, every element of it, and these element will clone every element of <i>their</i> collection and so on
     * @return A clone of this list with every element cloned too
     *
    ScriptValueCollection recursiveClone();
    */

    /**
     * to use {@link java.util.ArrayList} and {@link java.util.HashMap} again. <br/>
     * For instance, a {@link ScriptValueList} of {@link ScriptValue} of String will be converted to an {@code ArrayList<String>}.
     * @param forJson whether the keys of hashMaps should be a json of the real key, because in json, every key is a string.
     */
    Object toNormalClasses(boolean forJson);

    default Object toNormalClasses() {
        return toNormalClasses(false);
    }


    //TODO: think about a "static ScriptValueCollection toScriptValueList" here

    /**
     * The SQL table is composed by
     * <ol>
     *     <li>the prefix, given by {@link Storage}</li>
     *     <li>The type character:
     *         <ul>
     *             <li>{@code l} for {@link ScriptValueList}</li>
     *             <li>{@code d} for {@link ScriptValueMap}</li>
     *             <li>{@code s} for {@link StringScriptValueMap} although it should be and won't be used</li>
     *         </ul>
     *     </li>
     *     <li>the {@link StringID}</li>
     * </ol>
     * @see #getStringID()
     * @see #getTypeChar()
     * @return The SQL table used (its full name), {@code null} if the collection doesn't use SQL
     */
    @Nullable String getSQLTable();


    /**
     * Can be:
     * <ul>
     *     <li>{@code l} for {@link ScriptValueList}</li>
     *     <li>{@code d} for {@link ScriptValueMap}</li>
     *     <li>{@code s} for {@link StringScriptValueMap} although it should be and won't be used</li>
     * </ul>
     * <strong>This method must be overridden</strong>
     * @see #getSQLTable()
     */
    char getTypeChar();


    /**
     * As anything implementing {@link ScriptValueCollection} must have a {@link StringID}, this is for retrieving it.
     * @see #getSQLTable()
     */
    @NotNull StringID getStringID();

    /**
     * To know if this collection is using SQL
     * @return generally, for example for {@link ScriptValueList}, it's something like {@code this.arrayList == null}
     */
    boolean canUseSQL();

    /**
     * Use this with caution, it will try to make this collection using SQL. There is no way back.
     * @return if the collection was successfully to SQL
     * @throws NotUsingSQLException if the {@link Storage} given doesn't use SQL.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean makeSQL();

    int howManyTimesModifiedSinceLastSave();

    ScriptValueCollection clone();

    /**
     * Contrary of {@link ScriptValueCollection#transformToScriptValue(String, byte)}, this will be used to put
     * a {@link ScriptValue} <strong>in</strong> SQL.
     */
    static void setScriptValueInPreparedStatement(PreparedStatement ps, ScriptValue<?> element, int objectIndex, int typeIndex) throws SQLException {
        if (! Storage.isSQL) throw new NullPointerException("the storage class isn't using SQL");
        if (element == null || element.is(ScriptValue.SVType.NONE)) {
            ps.setString(objectIndex, null);
        } else if (element.v instanceof ScriptValueCollection) {
            ScriptValueCollection collection = (ScriptValueCollection) element.v;
            if (! collection.canUseSQL()) {
                collection.makeSQL();
            }
            ps.setString(objectIndex, collection.getStringID().toString());
        } else if (element.is(ScriptValue.SVType.BOOLEAN)) {
            ps.setString(objectIndex, element.asBoolean() ? "" : null);
        } else {
            ps.setString(objectIndex, element.toString());
        }
        ps.setByte(typeIndex, element != null ? element.type.asByte : 0);
    }

    /**
     * Contrary of {@link ScriptValueCollection#setScriptValueInPreparedStatement(PreparedStatement, ScriptValue, int, int)},
     * this will be used to retrieve a {@link ScriptValue} <strong>from</strong> SQL.
     */
    static ScriptValue<?> transformToScriptValue(String object, byte type) {
        switch (type) {
            case 0:
                return new ScriptValue<>(null);
            case 1:
                return new ScriptValue<>(Integer.valueOf(object));
            case 2:
                return new ScriptValue<>(Double.valueOf(object));
            case 3:
                return new ScriptValue<>(object);
            case 4:
                return new ScriptValue<>(object != null);
            case 5:
                ScriptValueList<?> pointedList = new ScriptValueList<>(new StringID(object));
                return new ScriptValue<>(pointedList);
            case 6:
                ScriptValueMap<?, ?> pointedMap = new ScriptValueMap<>(new StringID(object));
                return new ScriptValue<>(pointedMap);
            default:
                throw new IllegalStateException("Unexpected byteType: " + type);
        }
    }
}
