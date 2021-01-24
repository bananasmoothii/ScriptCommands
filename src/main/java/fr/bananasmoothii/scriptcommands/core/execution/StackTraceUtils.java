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

import fr.bananasmoothii.scriptcommands.core.CustomLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StackTraceUtils {

    private static final Pattern patternType = Pattern.compile("^(\\[*)(?:[^(]+\\.)?([^.(\\[<>]+(?:\\.<init>)?)(?:\\(.*\\))?$");

    private StackTraceUtils() { }

    /**
     * @param depth higher it is, deeper you will get until the first/main method. 0 will return the method that called the method that called <i>this</i> ({@link #getFromStackTrace(int)}).
     * @return the string of only the last name
     * @see #getLastName(String) the method used to get the last name of a class
     */
    public static String getFromStackTrace(int depth) {
        String function = "<unknown>";
        if (Thread.currentThread().getStackTrace().length >= depth + 3) {
            function = getLastName(String.valueOf(Thread.currentThread().getStackTrace()[depth + 2]));
        } else {
            CustomLogger.severe(String.format("INTERNAL ERROR: The stack trace is only %d long, could not take element #%d (%d + 2) from it. Returning \"<unknown>\" instead.%n",
                    Thread.currentThread().getStackTrace().length, depth + 2, depth));
        }
        return function;
    }

    /**
     * Method overloading that accepts any Object and calls {@link Object#getClass()}{@link Class#getName() .getName()} on it.
     * @see #getLastName(String)
     */
    public static String getLastName(Object object) {
        return getLastName(object.getClass().getName());
    }

    /**
     * Gets the last name following the regex {@code ^(\[*)(?:[^\(]+\.)?([^\.\(\[]+)(?:\(.*\))?$} and putting both group aside.
     * @param s name, e.g. {@code [[java.lang.Integer.valueOf(obj o)}
     * @return e.g. "<strong>{@literal [[valueOf}</strong>"
     */
    public static String getLastName(String s) {
        Matcher matcher = patternType.matcher(s);
        if (matcher.matches()) {
            return matcher.group(1) + matcher.group(2);
        }
        return "<ERROR while getting short name of " + s + ">";
    }

}
