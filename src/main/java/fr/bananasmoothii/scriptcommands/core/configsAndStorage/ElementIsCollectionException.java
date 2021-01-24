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

/**
 * For when <i>The element you wanted to add is the collection itself, and this is prohibited with the Scripts.</i>
 * @see ElementIsCollectionException#ElementIsCollectionException() the constructor whith the default message
 */
public class ElementIsCollectionException extends RuntimeException {

    /**
     * Constructs an exception with <i>The element you wanted to add is the collection itself, and this is prohibited with the Scripts.</i> as message
     */
    public ElementIsCollectionException() {
        super("The element you wanted to add is the collection itself, and this is prohibited with the Scripts.");
    }

    public ElementIsCollectionException(String message) {
        super(message);
    }
}
