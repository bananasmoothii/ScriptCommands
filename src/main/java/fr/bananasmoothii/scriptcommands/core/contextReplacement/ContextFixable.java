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
import org.jetbrains.annotations.Nullable;

/**
 * This interface means that you can fix a {@link Context} for an instance, and it will be always used instead of {@code null}
 * when no {@link Context} is given to the method. The main usage of {@link #setFixedContext(Context)} and
 * {@link #getFixedContext()} are methods in subclasses annotated {@link UseContext @UseContext}, passing
 * {@link #getFixedContext()} instead of just {@code null} to the method actually using a {@link Context} (as described
 * in {@link UseContext}.
 */
public interface ContextFixable {

    /**
     * Set the context that will always be used for this instance when calling a method annotated {@link UseContext @UseContext}
     */
    void setFixedContext(@Nullable Context context);

    @Nullable Context getFixedContext();

}
