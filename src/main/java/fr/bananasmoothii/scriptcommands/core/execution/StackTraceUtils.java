package fr.bananasmoothii.scriptcommands.core.execution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackTraceUtils {

    private static final Pattern patternType = Pattern.compile("^(\\[*)(?:[^(]+\\.)?([^.(\\[]+)(?:\\(.*\\))?$");

    private StackTraceUtils() { }

    /**
     * @param depth higher it is, deeper you will get until the first/main method. 0 will return the method that called the method that called <i>this</i> method.
     * @return the string of only the last name
     * @see StackTraceUtils#getLastName the method used to get the name last of a class
     */
    public static String getFromStackTrace(int depth) {
        String function = "<unknown>";
        if (Thread.currentThread().getStackTrace().length >= depth + 3) {
            function = getLastName(String.valueOf(Thread.currentThread().getStackTrace()[depth + 2]));
        } else {
            System.err.printf("INTERNAL ERROR: The stack trace is only %d long, could not take element #%d (%d + 2) from it. Returning \"<unknown>\" instead.%n",
                    Thread.currentThread().getStackTrace().length, depth + 2, depth);
        }
        return function;
    }

    /**
     * Method overloading that accepts any Object and calls {@link Object#getClass()}{@link Class#getName() .getName()} on it.
     * @see StackTraceUtils#getLastName(String)
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
