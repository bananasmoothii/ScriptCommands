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

package fr.bananasmoothii.scriptcommands.core;

import static fr.bananasmoothii.scriptcommands.core.CustomLogger.mainLogger;

/**
 * For making a parallel thread showing process of something like <br/>
 * <pre>{@code Parsing: 0%
 * Parsing: 62%
 * Parsing: 100%}</pre>
 *
 * @see ProgressPrinter#ProgressPrinter(String, double, int, boolean)
 */
public class ProgressPrinter extends Thread {

    private String prefix;
    private double delay;
    private int digitsShown;
    private double progressPercent = 0.0;
    private boolean cancelled = false;
    private final boolean autoMultiply;

    /**
     * Same as {@link ProgressPrinter#ProgressPrinter(String, double, int, boolean)} but with 0 for <i>digitsShown</i>,
     * 0.5 for <i>delay</i> and true for <i>autoMultiply</i>
     */
    public ProgressPrinter(String prefix) {
        this(prefix, 0.5, 0, true);
    }

    /**
     * Same as {@link ProgressPrinter#ProgressPrinter(String, double, int, boolean)} but with 0 for <i>digitsShown</i>
     * and true for <i>autoMultiply</i>
     */
    public ProgressPrinter(String prefix, double delay) {
        this(prefix, delay, 0, true);
    }

    /**
     * Same as {@link ProgressPrinter#ProgressPrinter(String, double, int, boolean)} but with true for <i>autoMultiply</i>
     */
    public ProgressPrinter(String prefix, double delay, int digitsShown) {
        this(prefix, delay, digitsShown, true);
    }

    /**
     * For making a parallel thread showing process of something like
     * <pre>{@code
     * Parsing: 0%
     * Parsing: 62%
     * Parsing: 100%}</pre>
     * {@link ProgressPrinter#setFinished()}
     * @param prefix like "Parsing: "
     * @param delay in seconds between two prints
     * @param digitsShown 0 for "12%", 1 for "12.3%" ...
     */
    public ProgressPrinter(String prefix, double delay, int digitsShown, boolean autoMultiply) {
        this.prefix = prefix;
        this.delay = delay;
        this.digitsShown = digitsShown;
        this.setName("Progress");
        this.start();
        this.autoMultiply = autoMultiply;
    }

    @Override
    public void run() {
        while (progressPercent < 100.0 && ! this.cancelled) {
            printSate();
            try {
                //noinspection BusyWait // because we're in a sperated thread
                Thread.sleep((long) (this.delay * 1000));
            } catch (InterruptedException ignored) { }
        }
        if (! this.cancelled)
            printSate(); // should be 100%
    }

    public void printSate() {
        CustomLogger.fine(prefix + round(progressPercent, digitsShown) + "% ");
    }

    /**
     * @return a String with x rounded to digitsShown digits
     */
    public static String round(double x, int digitsShown) {
        if (digitsShown == 0)
            return String.valueOf((int) x);

        double powOf10 = Math.pow(10, digitsShown);
        double n = ((int)(x * powOf10)) / powOf10;
        if (n == (int) n)
            return String.valueOf((int) n);
        return String.valueOf(n);
    }

    // ---- getters and setters ----

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    /**
     * @see ProgressPrinter#setFinished() finishing a process
     */
    public void setProgressPercent(double progressPercent) {
        if (autoMultiply) progressPercent *= 100;
        this.progressPercent = progressPercent;
    }

    /**
     * Same as {@link ProgressPrinter#setProgressPercent(double) setProgressPercent(100.0)} but will kill the tread
     * and print the progress a last time.<br/>
     * This method is recommended.
     */
    public void setFinished() {
        this.progressPercent = 100.0;

        printSate();
        this.cancelled = true;
    }

    public boolean getCancelled() {
        return this.cancelled;
    }

    public void cancel() {
        this.cancelled = true;
        CustomLogger.info("Cancelled");
    }

    public int getDigitsShown() {
        return digitsShown;
    }

    public void setDigitsShown(int digitsShown) {
        this.digitsShown = digitsShown;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }
}
