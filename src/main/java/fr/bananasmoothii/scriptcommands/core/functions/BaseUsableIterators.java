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

package fr.bananasmoothii.scriptcommands.core.functions;

import fr.bananasmoothii.scriptcommands.core.execution.Args;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptValue;

import java.util.Iterator;

@SuppressWarnings({"unchecked", "unused"})
public class BaseUsableIterators {

    @NamingPatternProvider
    public static final Args.NamingPattern range = new Args.NamingPattern()
            .setNamingPattern(1, "stop")
            .setNamingPattern("start", "stop", "step")
            .setDefaultValue("start", 0)
            .setDefaultValue("step", 1);

    @SuppressWarnings("rawtypes")
    @ScriptIteratorMethod
    public static Iterator<ScriptValue<?>> range(Args args) {
        ScriptValue<?> start = args.getArg("start"),
                stop  = args.getArg("stop"),
                step  = args.getArg("step");
        boolean allInt = start.is(ScriptValue.ScriptValueType.INTEGER) && stop.is(ScriptValue.ScriptValueType.INTEGER) && step.is(ScriptValue.ScriptValueType.INTEGER);
        if (allInt)
            return (Iterator<ScriptValue<?>>) (Iterator) new BaseUsableIterators.ScriptValueDoubleIterator(start.asInteger(), stop.asInteger(), step.asInteger()); // messy but best way //TODO: test
        return (Iterator<ScriptValue<?>>) (Iterator) new BaseUsableIterators.ScriptValueDoubleIterator(start.asDouble(), stop.asDouble(), step.asDouble());
    }

    public static class ScriptValueIntegerIterator implements Iterator<ScriptValue<Integer>> {

        private int start; // start is used as current value
        private final int stop, step;

        public ScriptValueIntegerIterator(int start, int stop, int step) {
            this.start = start;
            this.stop = stop;
            this.step = step;
        }

        @Override
        public boolean hasNext() {
            return start < stop;
        }

        @Override
        public ScriptValue<Integer> next() {
            int ret = start;
            start += step;
            return new ScriptValue<>(ret);
        }
    }

    public static class ScriptValueDoubleIterator implements Iterator<ScriptValue<Double>> {

        private double start;
        private final double stop, step;

        public ScriptValueDoubleIterator(double start, double stop, double step) {
            this.start = start;
            this.stop = stop;
            this.step = step;
        }

        @Override
        public boolean hasNext() {
            return start < stop;
        }

        @Override
        public ScriptValue<Double> next() {
            double ret = start;
            start += step;
            return new ScriptValue<>(ret);
        }
    }

    // TODO: more iterators
}
