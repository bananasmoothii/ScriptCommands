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

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is made for easily caching things. It is basically a {@code HashMap<String, Map<Object, Object>>}.
 * The first level is called "location", and the second "key"
 */
public class ScriptCache {

    private static HashMap<String, Map<Object, Object>> hashMap;

    public static @Nullable Object get(String location, Object key) {
        if (! hashMap.containsKey(location)) {
            hashMap.put(location, new HashMap<>());
            return null;
        }
        return hashMap.get(location).get(key);
    }

    public static @Nullable Map<Object, Object> getHoleMap(String location) {
        return hashMap.get(location);
    }

    public static void manualPut(String location, Map<Object, Object> map) {
        hashMap.put(location, map);
    }

}
