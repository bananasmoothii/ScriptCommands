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

/**
 * This class will contain every function. You can use it for registering functions
 * in {@link Context#registerScriptFunction(String, ScriptFunction, Args.NamingPattern)}.
 */
@FunctionalInterface
public interface ScriptFunction {

    ScriptValue<Object> run(Args args);

    class InvalidMethodException extends RuntimeException {
        public InvalidMethodException() {
            super();
        }
        public InvalidMethodException(String message) {
            super(message);
        }
    }
}
