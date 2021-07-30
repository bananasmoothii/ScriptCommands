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

package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.configsAndStorage.ScriptValueList;
import fr.bananasmoothii.scriptcommands.core.configsAndStorage.StringScriptValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

import static fr.bananasmoothii.scriptcommands.core.execution.ScriptException.ExceptionType.INVALID_ARGUMENTS;

public class Args {

    public static class Arg {

        public @Nullable String argName;
        public @NotNull ScriptValue<Object> value;

        public Arg(@Nullable String argName, @NotNull ScriptValue<Object> value) {
            this.argName = argName;
            this.value = value;
        }
    }

    public static class NamingPattern {
        private static final Pattern simplifiedVarNamePattern = Pattern.compile("[a-zA-Z_]\\w*");

        private final TreeMap<Integer, String[]> map = new TreeMap<>();
        public Map<String, ScriptValue<Object>> defaultValues = new HashMap<>();

        public NamingPattern setNamingPattern(int argNumber, String... argNames) {
            verifyArgNames(argNames);
            map.put(argNumber, argNames);
            return this;
        }

        public NamingPattern setNamingPattern(String... argNames) {
            verifyArgNames(argNames);
            map.put(Integer.MAX_VALUE, argNames);
            return this;
        }

        @Deprecated
        public NamingPattern setDefaultValues(Map<String, ScriptValue<Object>> defaultValuesMap) {
            defaultValues = defaultValuesMap;
            return this;
        }

        public NamingPattern setDefaultValue(String argName, ScriptValue<Object> defaultScriptValue) {
            defaultValues.put(argName, defaultScriptValue);
            return this;
        }

        public NamingPattern setDefaultValue(String argName, Object defaultValue) {
            defaultValues.put(argName, new ScriptValue<>(defaultValue));
            return this;
        }

        public static void verifyArgNames(String[] argNames) {
            for (String s: argNames) {
                if (!simplifiedVarNamePattern.matcher(s).matches())
                    throw new IllegalArgumentException('\"' + s + "\" is not a valid arg name");
            }
        }

        public @Nullable String[] getNamingPattern(int totalArgNumber) {
            return map.get(totalArgNumber);
        }

        public @Nullable String getNameForArg(int argNumber, int totalArgNumber) {
            String[] obj = map.get(getSmallestKeyAbove(totalArgNumber));
            if (obj == null || argNumber >= obj.length) return null;
            return obj[argNumber];
        }

        public int getSmallestKeyAbove(int totalArgNumber) {
            Integer higherKey = map.higherKey(totalArgNumber - 1);
            if (higherKey == null) return -1;
            return higherKey;
        }

        public int getBiggestNamingPatternLength() {
            try {
                return map.lastKey();
            } catch (NoSuchElementException e) {
                return -1;
            }
        }

        public boolean isDefinedArgName(String argName, int totalArgNumber) {
            return Arrays.asList(map.get(getSmallestKeyAbove(totalArgNumber))).contains(argName);
        }
    }


    private final ScriptValueList<Object> argsList;
    private final StringScriptValueMap<Object> argsMap;
    private @Nullable NamingPattern namingPattern;
    public @NotNull Context context;


    public Args(@NotNull Context context) {
        this(null, null, context);
    }

    public Args(@Nullable ScriptValueList<Object> argsList, @Nullable StringScriptValueMap<Object> argsMap, @NotNull Context context) {
        this.argsList = argsList == null ? new ScriptValueList<>() : argsList;
        this.argsList.setFixedContext(context);
        this.argsMap = argsMap == null ? new StringScriptValueMap<>() : argsMap;
        this.argsMap.setFixedContext(context);
        this.context = context;
    }

    public Args setNamingPattern(@Nullable NamingPattern namingPattern) {
        this.namingPattern = namingPattern;
        return this;
    }

    /**
     * This method tries to get an argument according to the {@link NamingPattern} (set in {@link #setNamingPattern(NamingPattern)}).
     * Note: as this can be a little slower than a simple getter, consider storing the result somewhere for not spamming that too much.
     * @param argName the name of an arg, specified by the {@link NamingPattern} or by the user with {@code arg1="some value"}
     * @return the matching {@link ScriptValue}
     * @throws ScriptException (type: {@link ScriptException.ExceptionType#INVALID_ARGUMENTS INVALID_ARGUMENTS})
     * @see #getArgIfExist(String) getArgIfExist(String) - if you just want a null if that arg doesn't exists.
     */
    public @NotNull ScriptValue<Object> getArg(String argName) {
        ScriptValue<Object> arg = getArgIfExist(argName);
        if (arg == null)
            throw new ScriptException(INVALID_ARGUMENTS, context,
                    "Argument " + argName + " was not defined but is mandatory.");
        return arg;
    }

    /**
     * This is the same as {@link #getArg(String)} except it doesn't throw an exception if the arg was not found,
     * but it returns {@code null}.
     */
    public @Nullable ScriptValue<Object> getArgIfExist(String argName) {
        if (namingPattern != null) {
            int size = argsList.size();
            for (int i = 0; i < size; i++) {
                String proposedName = namingPattern.getNameForArg(i, size);
                if (proposedName == null) break;
                if (proposedName.equals(argName)) return argsList.get(i);
            }
        }

        ScriptValue<Object> returnValue = argsMap.get(argName);
        if (returnValue != null) return returnValue;

        if (namingPattern != null) {
            returnValue = namingPattern.defaultValues.get(argName);
            return returnValue;
        }

        return null;
    }

    /**
     * This is an easier and faster way to get one single arguments, if you know there should be one. There is no
     * need for a {@link NamingPattern} if you use this.
     * @throws ScriptException (type: {@link ScriptException.ExceptionType#INVALID_ARGUMENTS INVALID_ARGUMENTS})
     * if there isn't one arg in total.
     */
    public @NotNull ScriptValue<Object> getSingleArg() {
        if (argsList.size() == 1 && argsMap.isEmpty()) return argsList.get(0);
        if (argsMap.size() == 1 && argsList.isEmpty()) return argsMap.values().get(0);
        throw new ScriptException(INVALID_ARGUMENTS, context, "There should be only one argument to this function.");
    }

    public @NotNull ScriptValueList<Object> getRemainingArgsList() {
        if (namingPattern == null) return new ScriptValueList<>();
        int biggestNamingPatternLength = namingPattern.getNamingPattern(namingPattern.getBiggestNamingPatternLength()).length;
        if (biggestNamingPatternLength >= argsList.size()) return new ScriptValueList<>();
        return argsList.subList(biggestNamingPatternLength, argsList.size());
    }

    public @NotNull StringScriptValueMap<Object> getRemainingArgsDictionary() {
        if (namingPattern == null) return new StringScriptValueMap<>();
        StringScriptValueMap<Object> result = new StringScriptValueMap<>();
        int size = argsList.size() + argsMap.size();
        for (String argName: argsMap.keySet()) {
            if (! namingPattern.isDefinedArgName(argName, size)) {
                result.put(argName, argsMap.get(argName));
            }
        }
        return result;
    }

    public void add(int index, ScriptValue<Object> arg) {
        argsList.add(index, arg);
    }

    public void add(ScriptValue<Object> arg) {
        argsList.add(arg);
    }

    public void add(@NotNull String argName, ScriptValue<Object> arg) {
        argsMap.put(argName, arg);
    }

    public void add(Arg arg) {
        if (arg.argName == null) argsList.add(arg.value);
        else argsMap.put(arg.argName, arg.value);
    }

    public @Nullable NamingPattern getNamingPattern() {
        return namingPattern;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean notFirstElement = false;
        if (argsList.size() == 0) {
            if (argsMap.size() == 0) return "";
            Iterator<Map.Entry<String, ScriptValue<Object>>> iterator = argsMap.entrySet().iterator();
            sb.append(iterator.next());
            while (iterator.hasNext()) {
                Map.Entry<String, ScriptValue<Object>> entry = iterator.next();
                if (notFirstElement) {
                    sb.append(", ");
                } else notFirstElement = true;
                sb.append(entry.getKey())
                        .append("=")
                        .append(Types.getPrettyArg(entry.getValue()));
            }
            return sb.toString();
        }
        for (ScriptValue<Object> scriptValue : argsList) {
            if (notFirstElement) {
                sb.append(", ");
            } else notFirstElement = true;
            sb.append(Types.getPrettyArg(scriptValue));
        }
        for (Map.Entry<String, ScriptValue<Object>> entry: argsMap.entrySet()) {
            //noinspection ConstantConditions
            if (notFirstElement) {
                sb.append(", ");
            } else notFirstElement = true;
            sb.append(entry.getKey())
                    .append("=")
                    .append(Types.getPrettyArg(entry.getValue()));
        }
        return sb.toString();
    }

    public ScriptValueList<Object> getArgsList() {
        return argsList;
    }

    public StringScriptValueMap<Object> getArgsMap() {
        return argsMap;
    }

    public boolean isEmpty() {
        return argsList.size() == 0 && argsMap.size() == 0;
    }
}
