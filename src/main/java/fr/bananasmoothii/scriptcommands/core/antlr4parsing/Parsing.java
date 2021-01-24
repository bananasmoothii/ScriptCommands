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

package fr.bananasmoothii.scriptcommands.core.antlr4parsing;

import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.execution.ScriptsParsingException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static fr.bananasmoothii.scriptcommands.core.CustomLogger.mainLogger;

public class Parsing {

    private static ParserDumper dumper;
    private static final Pattern patternFormattedScriptsLine = Pattern.compile("([^\\s<]+)(<([^>]*)>)? (.+)");
    private static boolean inPlaceholder, inText;

    public static ScriptsParser.StartContext parse(String name, String scripts) throws IOException, ScriptsParsingException {
        return parse(name, scripts, Charset.defaultCharset());
    }

    public static ScriptsParser.StartContext parse(String name, String scripts, Charset charset) throws IOException, ScriptsParsingException {
        InputStream stream = new ByteArrayInputStream(scripts.getBytes(charset));
        ScriptsLexer lexer = new ScriptsLexer(CharStreams.fromStream(stream));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ScriptsParser parser = new ScriptsParser(tokens);
        ScriptsParser.StartContext context = parser.start();
        if (context.exception != null || CustomLogger.getLevel().intValue() <= 300) {
            if (dumper == null)
                dumper = new ParserDumper();
            dumper.dumpParsing(name, scripts, lexer, parser, context);
        }
        if (context.exception == null) {
            return context;
        }
        throw new ScriptsParsingException("Parsing for " + name + " failed. See above message(s) for more info.");
    }

    public static PermissionParser.StartContext parsePermission(String commandName, String scripts) throws IOException, ScriptsParsingException {
        return parsePermission(commandName, scripts, Charset.defaultCharset());
    }

    public static PermissionParser.StartContext parsePermission(String commandName, String scripts, Charset charset) throws ScriptsParsingException, IOException {
        InputStream stream = new ByteArrayInputStream(scripts.getBytes(charset));
        PermissionLexer lexer = new PermissionLexer(CharStreams.fromStream(stream));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PermissionParser parser = new PermissionParser(tokens);
        PermissionParser.StartContext context = parser.start();
        if (context.exception == null) {
            return context;
        }
        throw new ScriptsParsingException("Parsing for permission of command " + commandName + " failed. See above message(s) for more info.");
    }

    public static String reformatScripts(ArrayList<String> linesFromConfig, Boolean directScripts) throws ScriptsParsingException {
        if (directScripts != Boolean.TRUE) {
            inPlaceholder = false;
            inText = false;
            for (int i = 0; i < linesFromConfig.size(); i++) {
                Matcher matcher = patternFormattedScriptsLine.matcher(linesFromConfig.get(i));
                if (matcher.matches()) {
                    if (! matcher.group(1).equals("script")) {
                        // replace the old string
                        if (matcher.group(3) != null)
                            linesFromConfig.set(i, String.format("%s(f\"%s\", [%s])",
                                    matcher.group(1), replaceQuotesExceptInPlaceholder(matcher.group(4)),
                                    matcher.group(3)));
                        else
                            linesFromConfig.set(i, String.format("%s(f\"%s\")",
                                    matcher.group(1), replaceQuotesExceptInPlaceholder(matcher.group(4))));
                    } else {
                        linesFromConfig.set(i, linesFromConfig.get(i).substring(7)); // for removing "script "
                    }
                }
                else {
                    throw new ScriptsParsingException("line " + i + " not recognized while reformatting the script line." +
                            " The regex for it is: " + patternFormattedScriptsLine);
                }
            }
        }
        return String.join("\n", linesFromConfig);

    }

    public static ScriptsParser.StartContext reformatAndParse(String name, ArrayList<String> linesFromConfig, Boolean directScripts) throws ScriptsParsingException, IOException {
        return parse(name, reformatScripts(linesFromConfig, directScripts));
    }

    /**
     * Not thread-safe
     */
    public static String replaceQuotesExceptInPlaceholder(String string) {
        StringBuilder sb = new StringBuilder();
        char before = ' '; // only to check with '{' so no problem
        for (char current : string.toCharArray()) {
            if (before == '{' && current == '=' && ! inPlaceholder) {
                inPlaceholder = true;
                sb.append('=');
            }
            else if (inPlaceholder && ! inText && current == '}') {
                inPlaceholder = false;
                sb.append('}');
            }
            else if (inPlaceholder && current == '"') {
                inText = !inText;
                sb.append('"');
            }
            else if (! inPlaceholder && current == '"')
                sb.append("\"\"");
            else
                sb.append(current);

            before = current;
        }
        return sb.toString();
    }
}
