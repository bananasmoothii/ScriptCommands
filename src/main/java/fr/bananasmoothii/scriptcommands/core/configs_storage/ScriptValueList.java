package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @param <E> the type of each {@link ScriptValue}
 */
public class ScriptValueList<E> extends AbstractList<ScriptValue<E>> implements Cloneable, ScriptValueContainer { //TODO: make this work

    private @Nullable Storage storage; // if null, will store in a normal ArrayList
    private boolean isSQL;
    private @Nullable String SQLTable; // with prefix, null if isSQL is false.
    private int timesModifiedSinceLastSave; // used only with json
    private @Nullable ArrayList<E> arrayList; // not null only with json


    public ScriptValueList() {
        this(null);
    }

    public ScriptValueList(@Nullable Storage storage) {
        this(storage, null);
    }

    /**
     *
     * @param storage if null, everything will be stored in a normal {@link ArrayList}
     * @param SQLTable without prefix
     */
    public ScriptValueList(@Nullable Storage storage, @Nullable String SQLTable) {
        super();
        this.storage = storage;
        isSQL = storage != null && storage.getMethod().contains("SQL");
        if (isSQL) {
            this.SQLTable = storage.getHashMap().get("table-prefix") + SQLTable;
        }
    }

    public static ScriptValueList<Object> toScriptValueArrayList(Object[] args) {
        ScriptValueList<Object> list = new ScriptValueList<>();
        for (Object arg: args) {
            list.add(new ScriptValue<>(arg));
        }
        return list;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void add(int index, ScriptValue<E> element) {
        if (isSQL) {
            try {
                PreparedStatement ps = storage.prepareSQLStatement("INSERT INTO global_variables VALUES(?, ?, ?)");
                ps.setString(1, String.valueOf(size()));
                if (element == null || element.is("None")) {
                    ps.setString(2, null);
                } else if (element.is("List")) {
                    // TODO
                } else if (element.is("Dictionary")) {
                    // TODO
                } else {
                    ps.setString(2, element.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean addAll(ScriptValueList<E> c) {
        return super.addAll(c);
    }

    /*@Override
    public boolean add(int index, E e) {
        return add(new ScriptValue<E>(e));
    }

    public boolean add(int index, ScriptValue<E> eScriptValue) {
        if (isSQL) {
            try {
                PreparedStatement ps = storage.prepareSQLStatement("INSERT INTO global_variables VALUES(?, ?, ?)");
                ps.setString(1, String.valueOf(size()));
                if (eScriptValue == null || eScriptValue.is("None")) {
                    ps.setString(2, null);
                } else if (eScriptValue.is("List")) {

                } else {
                    ps.setString(2, eScriptValue.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }*/

    @Override
    public ScriptValue<E> get(int index) {
        return null;
    }

    @Override
    public ScriptValueContainer recursiveClone() {
        return null;
    }

    @Override
    public ScriptValueList<E> clone()  {
        return null; // TODO OOOOOOOO everything here
    }

    @Override
    public ArrayList<ScriptValue<E>> toNormalClasses() {
        return null;
    }

    public static ScriptValueContainer toScriptValues(List<Object> list) {
        ScriptValueList<Object> finalList = new ScriptValueList<>();
        for (Object element: list) {
            ScriptValue<Object> scriptValueElement;
            if (element instanceof Map) scriptValueElement = new ScriptValue<>(ScriptValueMap.toScriptValues((Map<Object, Object>) element));
            else if (element instanceof List) scriptValueElement = new ScriptValue<>(ScriptValueList.toScriptValues((List<Object>) element));
            else if (element == null || element instanceof Integer || element instanceof Double || element instanceof String)
                scriptValueElement = new ScriptValue<>(element);
            else throw new IllegalArgumentException("the element " + element + " is not a valid type of ScriptValue, it is a " + element.getClass().getName());

            finalList.add(scriptValueElement);
        }
        return finalList;
    }
}
