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

package fr.bananasmoothii.scriptcommands.core.execution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Just a class to make catching ScriptException easier
 */
public abstract class AbstractScriptException extends RuntimeException {

    public AbstractScriptException(String message) {
        super(message);
    }

    public abstract String getStringType();

    public abstract void setStringType(String stringType);

    public abstract @Nullable String getGenericErrorDescription();

    /**
     * @return {@code this}, for chained call, e.g. {@code throw new ScriptException(...).setGenericErrorDescription(...);}
     */
    public abstract @NotNull AbstractScriptException setGenericErrorDescription(@Nullable String genericErrorDescription);
}
