/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.core.contextReplacement;


import fr.bananasmoothii.scriptcommands.core.execution.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation tells you that the method that you are about to use has an overload with a {@link Context}, and you
 * should use this one if you can grab a {@link Context} because it allows a better error handling.<br>
 * You can use a method annotated @UseContext with peace in mind if the class implements {@link ContextFixable} and you
 * set a context using {@link ContextFixable#setFixedContext(Context)}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UseContext {
}
