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

public class StringID {
    private final String chars;
    private final int charsLength;

    private final int intID;
    private final String stringID;

    public StringID(int intID) {
        chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        charsLength = 36;
        this.intID = intID;
        this.stringID = toString(intID);
    }

    public StringID(int intID, String usableChars) {
        chars = usableChars;
        charsLength = usableChars.length();
        this.intID = intID;
        this.stringID = toString(intID);
    }

    public StringID(String s) {
        chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        charsLength = 36;
        this.stringID = s;
        this.intID = toInt(s);
    }

    public StringID(String s, String usableChars) {
        chars = usableChars;
        charsLength = usableChars.length();
        this.stringID = s;
        this.intID = toInt(s);
    }

    public StringID nextID() {
        return new StringID(intID + 1);
    }

    @Override
    public String toString() {
        return stringID;
    }

    public String toString(int intID) {
        int floorDiv = Math.abs(intID);
        int modulo;
        StringBuilder sb = new StringBuilder();
        do {
            modulo = floorDiv % charsLength;
            floorDiv = floorDiv / charsLength;
            sb.insert(0, chars.charAt(modulo));
        } while (floorDiv != 0);
        if (intID < 0) sb.insert(0, '-');
        return sb.toString();
    }

    public int toInt() {
        return intID;
    }

    public int toInt(String string) {
        int res = 0;
        for (int i = 0; i < string.length(); i++) {
            res += Math.pow(charsLength, i) * chars.indexOf(string.charAt(string.length() - i - 1));
        }
        return res;
    }

    @Override
    public int hashCode() {
        return intID;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof StringID)) return false;
        return ((StringID) obj).intID == intID;
    }
}
