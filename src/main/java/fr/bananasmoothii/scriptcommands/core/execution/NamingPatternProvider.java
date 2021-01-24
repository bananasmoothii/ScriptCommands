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

import java.lang.annotation.*;

/**
 * This annotation is for <strong>public static final</strong> fields of type {@link fr.bananasmoothii.scriptcommands.core.execution.Args.NamingPattern}.
 * It will be used by {@link Context#registerMethodsFromObject(Object)} for making a
 * {@link fr.bananasmoothii.scriptcommands.core.execution.Args.NamingPattern} only once.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.FIELD)
public @interface NamingPatternProvider {
}
