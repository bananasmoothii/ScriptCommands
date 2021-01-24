package fr.bananasmoothii.scriptcommands.core.configs_storage;

import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// TODO
public class ScriptValueMap<K, V> extends AbstractMap<ScriptValue<K>, ScriptValue<V>> implements Cloneable, ScriptValueContainer {

    public ScriptValueMap() {
        super();
    }

    @NotNull
    @Override
    public Set<Entry<ScriptValue<K>, ScriptValue<V>>> entrySet() {
        return null; //TODO
    }

    @Override
    public ScriptValueContainer recursiveClone() {
        return null;
    }

    @Override
    public ScriptValueMap<K, V> clone() {
        return null;
    }

    @Override
    public Object toNormalClasses() {
        return null;
    }

    public static ScriptValueMap<Object, Object> toScriptValues(Map<Object, Object> map) {
        ScriptValueMap<Object, Object> finalMap = new ScriptValueMap<>();
        for (Entry<Object, Object> entry: map.entrySet()) {
            Object key = entry.getKey();
            ScriptValue<Object> scriptValueKey;
            if (key instanceof Map) scriptValueKey = new ScriptValue<>(ScriptValueMap.toScriptValues((Map<Object, Object>) key));
            else if (key instanceof List) scriptValueKey = new ScriptValue<>(ScriptValueList.toScriptValues((List<Object>) key));
            else if (key == null || key instanceof Integer || key instanceof Double || key instanceof String)
                scriptValueKey = new ScriptValue<>(key);
            else throw new IllegalArgumentException("the key " + key + " is not a valid type of ScriptValue, it is a " + key.getClass().getName());

            Object value = entry.getValue();
            ScriptValue<Object> scriptValueKValue;
            if (value instanceof Map) scriptValueKValue = new ScriptValue<>(ScriptValueMap.toScriptValues((Map<Object, Object>) value));
            else if (value instanceof List) scriptValueKValue = new ScriptValue<>(ScriptValueList.toScriptValues((List<Object>) value));
            else if (value == null || value instanceof Integer || value instanceof Double || value instanceof String)
                scriptValueKValue = new ScriptValue<>(value);
            else throw new IllegalArgumentException("the value " + value + " is not a valid type of ScriptValue, it is a " + value.getClass().getName());
            
            finalMap.put(scriptValueKey, scriptValueKValue);
        }
        return finalMap;
    }

    public String toString() {
        Iterator<Entry<ScriptValue<K>, ScriptValue<V>>> i = entrySet().iterator();
        if (! i.hasNext())
            return "[=]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            Entry<ScriptValue<K>, ScriptValue<V>> e = i.next();
            ScriptValue<K> key = e.getKey();
            ScriptValue<V> value = e.getValue();
            sb.append(key.v   == this ? "(this Map)" : key); // should be impossible to get as in the scripts, 2 variables can't reference the same object.
            sb.append('=');
            sb.append(value.v == this ? "(this Map)" : value);
            if (! i.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }
}
